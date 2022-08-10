package com.gzoltar.gradle;

import org.gradle.api.Project;
import org.gradle.api.Plugin;
import org.gradle.api.plugins.JavaPluginExtension;;
import org.gradle.api.file.FileCollection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;

import de.undercouch.gradle.tasks.download.Download;
import de.undercouch.gradle.tasks.download.DownloadSpec;

import com.gzoltar.cli.Main;

import java.io.PrintStream;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.net.URLClassLoader;
import java.net.URL;
import java.lang.reflect.Method;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Map;
import groovy.lang.Closure;

/**
 * The GZoltar gradle plugin
 */
public class GzoltarPlugin implements Plugin<Project> {
    public void apply(Project project) {
        //Register the gzoltar agent as a dependency and configuration
        /*Configuration super_impl = project.getConfigurations().getByName("implementation");
        ArrayList<Configuration> list = new ArrayList<Configuration>();
        list.add(super_impl);
        Configuration impl = project.getConfigurations().create("impl");
        impl.setExtendsFrom(list);
        project.getConfigurations().create("gzoltarAgent");
        project.getConfigurations().create("gzoltarCli");
        project.getDependencies().add("gzoltarAgent", "com.gzoltar:com.gzoltar.agent.rt:1.7.3");
        project.getDependencies().add("gzoltarCli", "com.gzoltar:com.gzoltar.cli:1.7.3");
        project.getDependencies().add("gzoltarCli", "junit:junit:4.11");
        project.getDependencies().add("gzoltarCli", "org.hamcrest:hamcrest-core:1.3");*/
        //Apply the GZoltar extension
        GzoltarPluginExtension extension = project.getExtensions()
          .create("gzoltar", GzoltarPluginExtension.class);
        //Define buildDir
        String buildDir = project.getBuildDir().getPath();
        SourceSetContainer sourceSet = (SourceSetContainer)(project.getProperties().get("sourceSets"));
        FileCollection testDirs = sourceSet.findByName("test").getOutput().getClassesDirs();
        FileCollection mainDirs = sourceSet.findByName("main").getOutput().getClassesDirs();
        //System.out.println("TestDirs: "+testDirs.getAsPath());
        //System.out.println("MainDirs: "+mainDirs.getAsPath());

        //Register the generate test list task
        project.getTasks().register("gzoltarGenTestList", task -> {
            task.dependsOn("compileTestJava");
            task.getOutputs().file(buildDir+"/"+extension.testMethods);
            task.doLast(new Action<Task>() {
                @Override
                public void execute(Task s) {
                  //JavaPluginExtension plugin = project.getExtensions().getByType(JavaPluginExtension.class);
                  //FileCollection testDirs = project.sourceSets.findByName("test").getOutput().getClassesDirs();
                  try {
                    for (File classDir : sourceSet.findByName("test").getRuntimeClasspath()) {
                      addToClassPath(classDir);
                    }
                    for (File testDir : testDirs) {
                      addToClassPath(testDir);
                    }
                    System.setSecurityManager(new NoExitSecurityManager());
                    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                    Main.main(new String[] {"listTestMethods", testDirs.getAsPath(),
                      "--outputFile", buildDir+"/"+extension.testMethods,
                      "--includes", extension.includes});
                  } catch (ExitException e) {
                    //All good
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                }
              }
            );
        });
        project.getTasks().register("gzoltarDownloads", Download.class, task -> {
          task.src(new String[] {
            "https://repo1.maven.org/maven2/com/gzoltar/com.gzoltar.cli/1.7.3/com.gzoltar.cli-1.7.3-jar-with-dependencies.jar",
              "https://repo1.maven.org/maven2/com/gzoltar/com.gzoltar.agent.rt/1.7.3/com.gzoltar.agent.rt-1.7.3-all.jar",
          });
          task.dest(buildDir);
          task.overwrite(false);

        });
        //Register the run tests task
        project.getTasks().register("gzoltarRunTests", task -> {
            task.dependsOn("gzoltarGenTestList");
            task.dependsOn("gzoltarDownloads");
            task.getOutputs().file(buildDir+"/"+extension.serFile);
            task.doFirst(new Action<Task>() {
              @Override
              public void execute(Task s) {
                  String separator = System.getProperty("file.separator");
                  Test test = (Test)project.getTasks().findByName("test");
                  //int i = 0;
                  //int len = test.getActions().size();
                  //System.out.println("Action queue length: "+len);
                  test.getActions().get(0).execute(test);
                  /*for(Action<? super Task> action : test.getActions()) {
                    if (i < len-2) {
                      action.execute(test);
                    }
                  }*/
                  try {
                    String GZAgent = "";
                    GZAgent = buildDir+"/com.gzoltar.agent.rt-1.7.3-all.jar";
                    //System.out.println("GZAgent: "+GZAgent);
                    //Configuration implementation = project.getConfigurations().getByName("impl");
                    String serFile = buildDir+"/"+extension.serFile;

                    ArrayList<String> args = new ArrayList<String>();

                    String OSType = System.getProperty("os.name").toLowerCase();
                    if (OSType.contains("windows")) {
                      args.add(System.getProperty("java.home")+".exe"
                          +separator+"bin"+separator+"java");
                    } else {
                      args.add(System.getProperty("java.home")
                          +separator+"bin"+separator+"java");
                    }

                    args.addAll(test.getJvmArgs());

                    args.add("-javaagent:"+GZAgent+"=destfile="+serFile+
                        ",buildlocation="+mainDirs.getAsPath());


                    args.add("-cp");
                    args.add(System.getProperty("java.class.path")+":"+
                        sourceSet.findByName("test").getRuntimeClasspath().getAsPath()+":"+
                        testDirs.getAsPath()+":"+mainDirs.getAsPath()+":"+
                        buildDir+"/com.gzoltar.cli-1.7.3-jar-with-dependencies.jar");
                        //gzoltarCli.getAsPath());

                    Map<String, Object> properties = test.getSystemProperties();
                    for (String property : properties.keySet()) {
                      Object value = properties.get(property);
                      args.add(String.format("-D%s=%s ", property, value));
                    }

                    args.add("com.gzoltar.cli.Main");
                    args.add("runTestMethods");

                    args.add("--testMethods");
                    args.add(project.getBuildDir().toPath()+"/"+extension.testMethods);

                    args.add("--collectCoverage");

                    args.add("--initTestClass");

                    System.out.println();
                    for (String arg : args) {
                      System.out.print(arg+" ");
                    }
                    System.out.println();

                    //System.out.println("Args: "+args);
                    runSeperateProg(args, null);
                  } catch (ExitException e) {
                    //All good
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                }
              }
            );
        });
        //Register the fault localization report task
        project.getTasks().register("gzoltarReport", task -> {
            task.dependsOn("gzoltarRunTests");
            task.getOutputs().dir(buildDir+"/sfl");
            task.doLast(new Action<Task>() {
                @Override
                public void execute(Task s) {
                  String serFile = buildDir+"/"+extension.serFile;
                  try {
                    System.setSecurityManager(new NoExitSecurityManager());
                    Main.main(new String[] {"faultLocalizationReport",
                      "--buildLocation", mainDirs.getAsPath(),
                      "--dataFile", serFile,
                      "--granularity", extension.granularity,
                      "--outputDirectory", buildDir});
                  } catch (ExitException e) {
                    //All good
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                }
              }
            );
        });
        project.getTasks().register("flitsr", task -> {
          task.dependsOn("gzoltarReport");
          task.doLast(new Action<Task>() {
            @Override
            public void execute(Task s) {
              ArrayList<String> args = new ArrayList<String>();
              args.add("flitsr");
              args.add(buildDir+"/sfl/txt");
              try {
                runSeperateProg(args, extension.outFile);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          });
        });
    }

    private static void addToClassPath(File file) throws Exception {
      Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
      method.setAccessible(true);
      method.invoke(Thread.currentThread().getContextClassLoader(), new Object[]{file.toURI().toURL()});
    }

    private static int runSeperateProg(ArrayList<String> args, String outFile) throws Exception {
      ProcessBuilder pb = new ProcessBuilder(args);
      pb.redirectErrorStream(true);;
      final Process p = pb.start();

      PrintStream out = null;
      if (outFile == null) {
        out = System.out;
      } else {
        out = new PrintStream(outFile);
      }
      InputStream is = p.getInputStream();
      BufferedInputStream isl = new BufferedInputStream(is);
      byte buffer[] = new byte[1024];
      int len = 0;
      while ((len = isl.read(buffer)) != -1) {
        out.write(buffer, 0, len);
      }

      int i = p.waitFor();
      if (i == 0) {
        return 0;
      }


      return 1;
    }
}
