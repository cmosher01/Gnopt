# Gnopt

Copyright © 2019, Christopher Alan Mosher, Shelton, Connecticut, USA, <cmosher01@gmail.com>.

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=CVSSQ2BWDCKQ2)
[![License](https://img.shields.io/github/license/cmosher01/gnopt.svg)](https://www.gnu.org/licenses/gpl.html)


Gnopt is a command-line option processor for Java programs. It handles GNU-style options:

```sh
--option=value
```

This software is distributed under the
[GPLv3](http://www.gnu.org/licenses/gpl-3.0-standalone.html)
license.

Include as a dependency in gradle:

```groovy
repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation group: 'nu.mine.mosher.gnopt', name: 'Gnopt', version: 'latest.release'
}
```

Example:

```java
package demo;

import nu.mine.mosher.gnopt.Gnopt;

import java.util.Optional;

public class Demo {


    /**
     * Command line option processor.
     * This is where you define and handle all command-line options.
     */
    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    public static class FoobarOpts {
        // Each public method becomes a command line option:

        // --something=whatever
        public void something(Optional<String> val) {
            foo = val.orElse("");
        }

        // --verbose
        public void verbose() {
            System.err.println("(verbose logging)");
        }

        // non-option arguments call this method (two underscores)
        public void __(String x) {
            System.err.println("arg: "+x);
        }



        // you can also store configuration state in instance variables
        private String foo = "default";
    }





    public static void main(String[] args) throws Throwable {
        // Pass args into Gnopt, and it will create a new instance,
        // and call methods for each argument present:
        FoobarOpts opts = Gnopt.process(FoobarOpts.class, args);

        // then use opts as you wish within the rest of your program
        System.err.println("something: "+opts.foo);
    }



}
```

```sh
java -cp ... demo.Demo   --something=testing --verbose infile
(verbose logging)
arg: infile
something: testing
```
