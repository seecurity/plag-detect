#Plagiatsdetector
Plagiatsdetector is a tool written in java that automatically scans though folders of files and checks all files of a specified extension for equal occurences of a certain length.

### Authors
Sebastian Schinzel, Dennis Loehr

### Version
1.0

### Installation
The easiest Setup is to use eclipse. Download the eclipse IDE for Java according to your system from here: https://www.eclipse.org/downloads/
Import the project via File->Import...->Existing Projects into Workspace. You will now find the Project in your workspace. You will need to modify the file ```src/de.fhmuenster.its.plag/Main.java``` according to your needs.

#### Configuration
- Line 47: The Path variable. Put the path with all the sources here. You may want to put them in different directories based on the authors.
- Line 51: The extensions variable. This is a list of extensions you want to be checked. For instance if you check eclipse projects for code duplication you may want to skip automatically produced files such as '.project' and '.classpath'. The list is specified by entries surrounded by "". They are seperated by a comma and you can extend it.
- Line 55: The blacklist variable. This list contains all the files that you specifically not want to be checked. For instance this could be several files again produced by your IDE such as 'settings.xml' or something.

#### Run

Just click run in eclipse or build the java file manually and execute it from console. The outputs will be as following:
```
> Dirname: example1
> length: 5302
>
> Dirname: example2
> length: 4524
>
> Job 0 out of 4 started 
> [...] 
> example1:example2= 0,175066
> example2:example1= 0,175066
```

Explaination:
Two directories were compared, one containing 5302 Bytes, one 4525. Then several Jobs were started to handle the comparison. Finally the file example1 and example2 have a similarity of 0,175066 wich means 17.5066% are equal.
Additionally the program will create '.html' and '.csv'-Files in the default directory (either the directory you run it from or if run by eclipse in the project directory) to visualize the passages found in other files.

### Troubleshooting
- Q: Why am I not getting reuslts for certain files?
- A: Files that contain less than 1000 chars will not be checked for.
