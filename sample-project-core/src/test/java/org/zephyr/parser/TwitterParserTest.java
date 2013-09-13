package org.zephyr.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.zephyr.data.Pair;
import org.zephyr.data.ProcessingResult;

public class TwitterParserTest {
    
    private Parser parser;
    
    @Before
    public void setup() {
        parser = new TwitterParserFactory().newParser(ClassLoader.getSystemClassLoader().getResourceAsStream("twitter-sample.tsv"));
    }
    
    @Test
    public void testParser() throws IOException {
        ProcessingResult<List<Pair>, byte[]> result = null;
        List<List<Pair>> results = new ArrayList<List<Pair>>();
        while((result = parser.parse()) != null) {
            if (result.wasProcessedSuccessfully())
                results.add(result.getProcessedData());
            else
                //System.out.println(results.size());
                result.getError().printStackTrace();
        }

        assertEquals(5,results.size());

        for (int i = 0; i < 5; i++) {
            for (Pair pair : results.get(i)) {
                System.out.print(pair.getKey());
                System.out.print(pair.getValue());
                System.out.print("\t");
            }
            System.out.print("\n");
        }
    }
    
    @Test
    public void testSplit() {
        String testString = "\t";
        String values [] = testString.split("\t");
        System.out.println(values.length);
    }

}
