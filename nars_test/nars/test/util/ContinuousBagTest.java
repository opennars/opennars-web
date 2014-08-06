package nars.test.util;

import nars.perf.BagPerf.NullItem;
import nars.util.ContinuousBag;
import org.junit.Test;

/**
 *
 * @author me
 */


public class ContinuousBagTest {
 
    @Test 
    public void testContinuousBag() {
        testFastBag(false);
        testFastBag(true);
        
        testFastBagCapacityLimit(false);
        testFastBagCapacityLimit(true);
        
        
        
        testRemovalDistribution(4, false);
        testRemovalDistribution(4, true);
        
        testRemovalDistribution(7, false);
        testRemovalDistribution(7, true);
        
        testRemovalDistribution(16, false);
        testRemovalDistribution(16, true);

        testRemovalDistribution(13, false);
        testRemovalDistribution(13, true);
        
    }
    
    public void testFastBag(boolean random) {
        ContinuousBag<NullItem> f = new ContinuousBag(4, 10, random);
        
        f.putIn(new NullItem(.25f));
        assert(f.size() == 1);
        assert(f.getMass() > 0);
        
        f.putIn(new NullItem(.9f));
        f.putIn(new NullItem(.75f));
        
        //System.out.println(f);
        
        //sorted
        assert(f.items.get(0).getPriority() < f.items.get(1).getPriority());

        assert(f.size() == 3);
        f.takeOut();
        
        assert(f.size() == 2);
        f.takeOut();
        assert(f.size() == 1);
        f.takeOut();
        assert(f.size() == 0);
        
        assert(f.getMass() == 0);
    }

    public void testFastBagCapacityLimit(boolean random) {
        ContinuousBag<NullItem> f = new ContinuousBag(4, 10, random);
        f.putIn(new NullItem());
        f.putIn(new NullItem());
        f.putIn(new NullItem());
        boolean a = f.putIn(new NullItem());
        assert(a);
        f.putIn(new NullItem()); //limit
        assert(f.size() == 4);
        f.putIn(new NullItem()); //limit
        assert(f.size() == 4);

    }
    
    public void testRemovalDistribution(int N, boolean random) {
        int samples = 256 * N;
        
        int count[] = new int[N];
        
        ContinuousBag<NullItem> f = new ContinuousBag(N, 10, random);
        
        //fill
        for (int i= 0; i < N; i++) {
            f.putIn(new NullItem());
        }
        
        for (int i= 0; i < samples; i++) {
            count[f.nextRemovalIndex()]++;
        }
                
        //removal rates are approximately monotonically increasing function
        //assert(count[0] <= count[1]);
        assert(count[0] < count[N-1]);        
        //assert(count[N-2] < count[N-1]);        
        assert(count[N/2] <= count[N-1]);
        
        //System.out.println(random + " " + Arrays.toString(count));
        //System.out.println(count[0] + " " + count[1] + " " + count[2] + " " + count[3]);
        
    }
    
}
