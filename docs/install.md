This document will outline how to install Manzan on to IBM i. Manzan is install through a pase shell. We don't recommend using `qsh` or `qp2term`, and instead using SSH and a bash shell.

Building Manzan is easy for IBM i and we provide makefiles to simplify the entire process.

## Which Java to use
Use a Java version provided by IBM, which is at least version 8. When running the `java -version` command, the output 
should contain the string `IBM`. Ex. `IBM Semeru Runtime Certified Edition`. Otherwise, Manzan may not function properly.

## Install from GitHub release 

To install from a github release, simply perform the following steps:
1. Download the latest binary release from [the releases page](https://github.com/ThePrez/Manzan/releases)
2. If you didn't download to IBM i directly, transfer the `.jar` file to IBM i using technique of your choice
3. Run `java -jar <name of jar file>`

For instance, to install version `0.0.6`, the steps from an IBM i (using open source `wget`) would look like:
```bash
wget https://github.com/ThePrez/Manzan/releases/download/v0.0.6/manzan-installer-v0.0.6.jar
java -jar manzan-installer-v0.0.6.jar
```

## Deployment basics

* The configuration files (`.ini` extension) get created in `/QOpenSys/etc/manzan/`
* Manzan can be started with `/opt/manzan/bin/manzan`
* (optional) It is recommended to use [Service Commander](https://theprez.github.io/ServiceCommander-IBMi/#service-commander-for-ibm-i) to start/stop Manzan (or to have Manzan autostart at IBM)

## Next steps

* Read more about the [configuration files](/config/index.md)
* Build your very first [handler](/examples/file.md)