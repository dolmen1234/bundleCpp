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
//        new Bundler().compile(target);
    }

    @Test
    public void testRegexp() throws IOException {
        String line = "#define LL_AND3(a,b,c,d) (a) &= (d) | (b) | (c)"; 
        String clean = Bundler.cleanLine(line);
        System.err.println(line);
        System.err.println(clean);
    }

    
}
