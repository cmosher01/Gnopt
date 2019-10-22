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

Include as dependency in gradle:

```groovy
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation group: 'nu.mine.mosher.gnopt', name: 'gnopt', version: 'latest.release'
}
```

Example:

```java
import nu.mine.mosher.gnopt.Gnopt;

public class Foobar {
    public static void main(String[] args) {
        // Pass args into Gnopt, and it will create a new instance,
        // and call methods for each arg present:
        FoobarOpts opts = Gnopt.process(FoobarOpts.class, args);

        // then use opts as you wish within the rest of your program
    }

    public static class FoobarOpts {
        public String foo = "default";

        // each method becomes a command line option

        // --something=whatever
        public void something(Optional<String> val) {
            foo = val.orElse("");
        }

        // --verbose
        public void verbose() {
            System.err.println("(verbose logging)");
        }

        // non-option arguments call this method (two underscores)
        void __(String x) {
            System.err.println("arg: "+x);
        }
    }
}
```
