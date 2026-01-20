import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Index2DTest {

    @Test
    void testDefaultCtor() {
        Index2D p = new Index2D();
        assertEquals(0, p.getX());
        assertEquals(0, p.getY());
    }

    @Test
    void testCtorAndGetters() {
        Index2D p = new Index2D(3, 7);
        assertEquals(3, p.getX());
        assertEquals(7, p.getY());
    }

    @Test
    void testCopyCtor() {
        Pixel2D a = new Index2D(1, 2);
        Index2D b = new Index2D(a);
        assertEquals(1, b.getX());
        assertEquals(2, b.getY());
        assertEquals(a, b);
    }

    @Test
    void testDistanceZero() {
        Index2D a = new Index2D(5, 5);
        Index2D b = new Index2D(5, 5);
        assertEquals(0.0, a.distance2D(b), 1e-9);
    }

    @Test
    void testDistance345() {
        Index2D a = new Index2D(0, 0);
        Index2D b = new Index2D(3, 4);
        assertEquals(5.0, a.distance2D(b), 1e-9);
    }

    @Test
    void testDistanceSymmetry() {
        Index2D a = new Index2D(2, 9);
        Index2D b = new Index2D(-1, 3);
        assertEquals(a.distance2D(b), b.distance2D(a), 1e-9);
    }

    @Test
    void testDistanceNullThrows() {
        Index2D a = new Index2D(0, 0);
        assertThrows(NullPointerException.class, () -> a.distance2D(null));
    }

    @Test
    void testToString() {
        Index2D a = new Index2D(8, -2);
        assertEquals("8,-2", a.toString());
    }

    @Test
    void testEquals() {
        Index2D a = new Index2D(1, 1);
        Index2D b = new Index2D(1, 1);
        Index2D c = new Index2D(1, 2);

        assertEquals(a, a);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "not a pixel");
    }

    @Test
    void testHashCodeConsistencyWithEquals() {
        Index2D a = new Index2D(7, 9);
        Index2D b = new Index2D(7, 9);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
