This document will outline how to install Manzan on to IBM i. Manzan is install through a pase shell. We don't recommend using `qsh` or `qp2term`, and instead using SSH and a bash shell.

Building Manzan is easy for IBM i and we provide makefiles to simplify the entire process.

## Requirements

To build Manzan, GNU Make (`gmake`), Java and Maven must be setup and on the path so the shell can access them.

## Install from GitHub release

(coming at a later date)

## Build from source (for Development)

1. `cd Manzan`
   * ensure that the working directory is the root of Manzan
2. `gmake install` - installs the Handler (ILE component) into the `MANZAN` library
    * change installation library with `BUILDLIB=MANZAN2 gmake install`

After you install Manzan with the makefiles:

* Both the Handler and Distributor are built
* The configuration files (`.ini` extension) get created in `/QOpenSys/etc/manzan/`
* Manzan can be started with `/opt/manzan/bin/manzan`

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

## Next steps

* Read more about the [configuration files](/config/index.md)
* Build your very first [handler](/examples/file.md)