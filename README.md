# Gnopt

Copyright © 2019, 2022, Christopher Alan Mosher, Shelton, Connecticut, USA, <cmosher01@gmail.com>.

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
    mavenCentral()
    maven {
        url = uri('https://public:\u0067hp_fya6Kseu3XOBMg2icbg7f1LP6ZFYjj35v4Zj@maven.pkg.github.com/cmosher01/*')
    }
}

dependencies {
    implementation group: 'nu.mine.mosher.gnopt', name: 'gnopt', version: 'latest.release'
}
```

Example:

```java
package demo;

import nu.mine.mosher.gnopt.Gnopt;

import java.util.Optional;

/**
 * Command line option processor.
 * This is where you define and handle all your command-line options.
 *
 * It is just a POJO.
 * Each public, non-static, method becomes a command line option.
 *
 * Method requirements:
 *
 *     1. must be public
 *     2. must have void return type
 *     3. must have one and only one Optional<String> argument
 *     4. cannot be abstract
 *
 *  Turn on logging (slf4j) to see any error messages.
 */
@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
public class FoobarOpts {
    // --something=whatever
    public void something(Optional<String> val) {
        foo = val.orElse("");
    }

    // non-option arguments call this method (two underscores)
    public void __(Optional<String> val) {
        System.err.println("arg: "+val.get());
    }

    // you can store configuration state in instance variables
    public String foo = "default";



    // The main program can be anywhere; it doesn't need to
    // be in this class.
    public static void main(String[] args) throws Throwable {
        // Pass args into Gnopt, and it will create a new instance
        // and call methods on it for each argument:
        FoobarOpts opts = Gnopt.process(FoobarOpts.class, args);

        // then use opts as you wish within the rest of your program
        System.err.println("something: "+opts.foo);
    }
}
```

```sh
java -cp ... demo.Demo   --something=testing infile
arg: infile
something: testing
```
