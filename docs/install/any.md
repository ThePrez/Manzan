This document will cover installing on non-IBM i environments, such as Windows or Linux.

When running Manzan on non-IBM i environments, the provided makefiles cannot be used. The Handler (ILE component) can only be run on IBM i and the Distributor (Camel component) can run anywhere.

## Requirements

To build the camel component, Java and Maven must be setup and on the path so the shell can access them.

## Build

1. `cd Manzan/camel`
2. `mvn compile`
   * builds camel into the `camel/target` directory
3. `mvn exec:java`
   * this starts the Distributor up on the system.
   * If it is run for the first time, it will create empty configuration files (`.ini` extension) into the working directory

## Next steps

* Read more about the [configuration files](/config/index.md)
* Build your very first [handler](/examples/file.md)