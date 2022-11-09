package com.notcat;

import com.notcat.patching.Patcher;
import com.notcat.patching.transformers.impl.*;

import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        if(args.length != 1) {
            System.err.println("Provide path to charles.jar as the first argument");
            System.exit(-1);
            return;
        }

        Patcher patcher = new Patcher(args[0]);
        if (patcher.initialize())
            if (patcher.applyTransformers(
                    Paths.get("./patched-charles.jar"),
                    new DemoTransformer(),
                    new JA3Transformer(),
                    new HeaderKeysTransformer(),
                    new ContextTransformer(),
                    new JSSETransformer()
            ))
                System.out.println("All transformers applied successfully");
            else
                System.out.println("Failed to apply all transformers");

    }

}
