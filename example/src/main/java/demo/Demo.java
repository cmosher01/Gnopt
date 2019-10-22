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
        // and call methods for each arg present:
        FoobarOpts opts = Gnopt.process(FoobarOpts.class, args);

        // then use opts as you wish within the rest of your program
        System.err.println("something: "+opts.foo);
    }



}
