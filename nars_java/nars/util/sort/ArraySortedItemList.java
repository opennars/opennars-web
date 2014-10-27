package nars.util.sort;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import nars.entity.Item;

//public class PrioritySortedItemList<E extends Item> extends GapList<E>  {    
//public class PrioritySortedItemList<E extends Item> extends ArrayList<E>  {    
//abstract public class SortedItemList<E> extends FastTable<E> {
public class ArraySortedItemList<E extends Item> extends ArrayList<E> implements SortedItemList<E> {

    int capacity = Integer.MAX_VALUE;
    private List<E> reverse;

//    public ArraySortedItemList(Comparator<E> c) {
//        this(c, Integer.MAX_VALUE);
//    }
//
//    public ArraySortedItemList(Comparator<E> c, int capacity) {
//        super();
//        //this.comparator = c;
//        this.capacity = capacity;
//    }
    public ArraySortedItemList() {
        this(1);
    }

    public ArraySortedItemList(int capacity) {
        super();
        this.capacity = capacity;
        /*
         this(new Comparator<E>() {

         @Override public int compare(final E o1, final E o2) {
         //fast comparison because name table will already prevent duplicates?
         if (o1 == o2) return 0;
         return -1;
         }
                
         },capacity);*/
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public final int positionOf(final E o) {
        final float y = o.budget.getPriority();
        final int s = size();
        if (s > 0) {

            //binary search
            int low = 0;
            int high = s - 1;

            while (low <= high) {
                int mid = (low + high) >>> 1;

                E midVal = get(mid);

                final float x = midVal.budget.getPriority();

                if (x < y) {
                    low = mid + 1;
                } else if (x == y) {
                    return mid;
                } else if (x > y) {
                    high = mid - 1;
                }

            }
            return low;
        } else {
            return 0;
        }
    }

    @Override
    public boolean add(final E o) {
        
        if (isEmpty()) {
            return super.add(o);
        } else {
            if (size() == capacity) {

                if (positionOf(o) == 0) {
                    //priority too low to join this list
                    return false;
                }

                reject(remove(0));
            }
            super.add(positionOf(o), o);
            return true;
        }
    }

    @Override
    public E getFirst() {
        if (isEmpty()) {
            return null;
        }
        return get(0);
    }

    @Override
    public E getLast() {
        if (isEmpty()) {
            return null;
        }
        return get(size() - 1);
    }

    public int capacity() {
        return capacity;
    }

    public int available() {
        return capacity() - size();
    }

    @Override
    public Iterator<E> descendingIterator() {
        if (reverse == null) {
            reverse = Lists.reverse(this);
        }
        return reverse.iterator();
    }

    /**
     * can be handled in subclasses
     */
    protected void reject(E removeFirst) {
    }

}
/*
 public class PrioritySortedItemList<E extends Item> extends SortedList<E> {

 public PrioritySortedItemList() {
 super(null);
 }

 @Override
 public boolean add(final E o) {
        
 final int y = o.budget.getPriorityShort();
        
 if (size() > 0)  {
            
 //binary search
 int low = 0;
 int high = size()-1;

 while (low <= high) {
 int mid = (low + high) >>> 1;
 E midVal = get(mid);
                
 final int x = midVal.budget.getPriorityShort();
 int cmp = (x < y) ? -1 : ((x == y) ? 0 : 1);                   

 if (cmp < 0)
 low = mid + 1;
 else if (cmp > 0)
 high = mid - 1;
 else {
 // key found, insert after it
 super.add(mid, o);
 return true;
 }
 }
 super.add(low, o);
 return true;
 }
 else {
 super.add(0,o);
 return true;
 }
 }

    
    
    
 }
 */