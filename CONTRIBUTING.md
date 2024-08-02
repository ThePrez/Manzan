## Contributing to Manzan

We welcome everyone to work on this cool project. We use a typical PR system:

1. You fork and clone
2. Commit and push changes to your fork
3. Make a pull request (PR)

## Development environment

### Visual Studio Code

1. Clone your fork to your local machine
2. Open the repository up in Visual Studio Code
3. Connect to a remote IBM i where the build can happen
4. Correctly set your current library to where your objects can be built
   * Code for IBM i will tell you what you have it set to when you have connected to a system.
5. Set your Deploy Workspace Location in the IFS
   * Code for IBM i can set a default location if you have not yet done it before, OR
   * Right click on the chosen directory and select 'Set Deploy Workspace Location'.
   * You only have to do this once.
6. Actions are available to compile source from your local machine
   * Control / Command + E


## Build from source (for development/contribution)

1. `cd Manzan`
   * ensure that the working directory is the root of Manzan
2. `gmake install` - installs the Handler (ILE component) into the `MANZAN` library
    * change installation library with `BUILDLIB=MANZAN2 gmake install`

After you install Manzan with the makefiles:

* Both the Handler and Distributor are built
* The configuration files (`.ini` extension) get created in `/QOpenSys/etc/manzan/`
* Manzan can be started with `/opt/manzan/bin/manzan`


## Building the Distributor component only via Maven

1. `cd Manzan/camel`
2. `mvn compile`
   * builds camel into the `camel/target` directory
3. `mvn exec:java`
   * this starts the Distributor up on the system.
   * If it is run for the first time, it will create empty configuration files (`.ini` extension) into the working directory


### Build Distributor (camel) only

To build only the camel component, you can use `camel` as the target:

```sh
gmake camel
```

### Build Handler (ILE) only

To build only the ILE component, you can use `ile` as the target:

```sh
gmake ile
```

### Recommended tools for development 
- ShellCheck vscode extension
- act https://github.com/nektos/act (github actions local runner)
- c/c++ vscode extensions
- Code Spell Checker

### Running integration tests
- First copy the .env.sample file to .env and fill out the values
- Run tests by executing one of the following tasks 
    - deployAndTest: Sync the files to your IBM i, and run the tests
    - deployBuildAllAndTest: Sync the files to your IBM i, build manzan including the ile component and run the tests
    - deployBuildCamelAndTest: Sync the files to your IBM i, only build the java code and then run the tests

### Running unit tests
- Run the task `ileUnitTest`

