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
   * F1 -> 'change current library' command
5. Set your Deploy Workspace Location in the IFS
   * Recommended to create a new directory in your home directory
   * Right click on the chosen directory and select 'Set Deploy Workspace Location'
   * You only have to do this once.
6. Actions are available to compile source from your local machine
   * Control / Command + E