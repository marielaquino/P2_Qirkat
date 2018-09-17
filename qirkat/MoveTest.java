/* Author: Paul N. Hilfinger.  (C) 2008. */

package qirkat;

import org.junit.Test;
import static org.junit.Assert.*;

import static qirkat.Move.*;

/** Test Move creation.
 *  @author Mariel Aquino
 */
public class MoveTest {

    @Test
    public void testMove1() {
        Move m = move('a', '3', 'b', '2');
        assertNotNull(m);
        assertFalse("move should not be jump", m.isJump());
    }

    @Test
    public void testJump1() {
        Move m = move('a', '3', 'a', '5');
        assertNotNull(m);
        assertTrue("move should be jump", m.isJump());
    }

    @Test
    public void testString() {
        assertEquals("a3-b2", move('a', '3', 'b', '2').toString());
        assertEquals("a3-a5", move('a', '3', 'a', '5').toString());
        assertEquals("a3-a5-c3", move('a', '3', 'a', '5',
                                      move('a', '5', 'c', '3')).toString());
    }


    @Test
    public void testParseString() {
        assertEquals("a3-b2", parseMove("a3-b2").toString());
        assertEquals("a3-a5", parseMove("a3-a5").toString());
        assertEquals("a3-a5-c3", parseMove("a3-a5-c3").toString());
        assertEquals("a3-a5-c3-e1", parseMove("a3-a5-c3-e1").toString());
    }

    @Test
    public void testIsLeftMove() {
        Move m = move('b', '2', 'a', '2');
        Move b = move('a', '3', 'b', '3');
        Move c = move('c', '3', 'a', '3');
        assertEquals(true, m.isLeftMove());
        assertEquals(false, b.isLeftMove());
        assertEquals(false, c.isLeftMove());
    }
    @Test
    public void testIsRightMove() {
        Move m = move('a', '2', 'b', '2');
        Move b = move('b', '3', 'a', '3');
        Move c = move('a', '3', 'c', '3');
        assertEquals(true, m.isRightMove());
        assertEquals(false, b.isRightMove());
        assertEquals(false, c.isRightMove());
    }

    @Test
    public void testJumpedRow() {
        Move m = move('a', '3', 'a', '5');
        Move j = move('a', '3', 'c', '3');
        assertEquals('4', m.jumpedRow());
        assertEquals('a', m.jumpedCol());
        assertEquals('3', j.jumpedRow());
        assertEquals('b', j.jumpedCol());

    }


}
