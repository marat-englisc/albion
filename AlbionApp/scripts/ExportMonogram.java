import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.nio.file.*;

/** Exports the supplied Caslon A, preserving its actual outline in the Android icon. */
class ExportMonogram {
    public static void main(String[] args) throws Exception {
        Font font = Font.createFont(Font.TRUETYPE_FONT, Path.of(args[0]).toFile()).deriveFont(100f);
        var glyph = font.createGlyphVector(new FontRenderContext(null, true, true), "A").getOutline();
        var bounds = glyph.getBounds2D();
        double scale = 40 / bounds.getHeight();
        var transform = new AffineTransform();
        transform.translate(54 - bounds.getCenterX() * scale, 54 - bounds.getCenterY() * scale);
        transform.scale(scale, scale);
        StringBuilder path = new StringBuilder();
        double[] c = new double[6];
        for (var it = glyph.getPathIterator(transform); !it.isDone(); it.next()) {
            int type = it.currentSegment(c);
            String command = new String[]{"M", "L", "Q", "C", "Z"}[type];
            path.append(command);
            int count = new int[]{2, 2, 4, 6, 0}[type];
            for (int i = 0; i < count; i++) path.append(String.format(java.util.Locale.US, "%.3f", c[i])).append(i+1 == count ? " " : ",");
        }
        String vector = """
            <?xml version="1.0" encoding="utf-8"?>
            <!-- Actual outline of the project's Adobe Caslon Pro A; 108 dp adaptive safe zone. -->
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
                <path android:pathData="M28,28H80V80H28Z" android:strokeColor="#C6A66D" android:strokeWidth="0.7" />
                <path android:pathData="%s" android:fillColor="#F6F1E7" />
                <path android:pathData="M44,80H64" android:strokeColor="#C6A66D" android:strokeWidth="1.5" />
            </vector>
            """.formatted(path);
        Files.writeString(Path.of(args[1]), vector);
        System.out.println("Exported Adobe Caslon monogram.");
    }
}
