package fr.dgigon.codg.bundler;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class BundleTest {

    @Test
    public void testBundle() throws IOException, InterruptedException {
        File src = new File("D:\\workspaces\\cdg\\contestCpp");
        File target = new File("D:\\workspaces\\cdg\\contestCpp\\sync\\Player.cpp");
        new Bundler().bundle(src, target, Bundler.exludeDirNames, "contestCpp.cpp");
        new Bundler().compile(target);
    }

    
}
