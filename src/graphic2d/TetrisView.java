package graphic2d;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * 2D Graphic Radial Tetris version
 * 
 * @author leonardo
 */
public class TetrisView extends JFrame {
    
    private TetrisModel model = new TetrisModel();
    
    private BufferedImage grid;
    private int[] gridData;
    
    private BufferedImage offscreen;
    private int[] offscreenData;
    
    private Font font = new Font("Arial", Font.PLAIN, 20);
    
    private Color[] colors = { 
        Color.BLACK, 
        new Color(255, 0, 0), 
        new Color(0, 255, 0), 
        new Color(0, 0, 255), 
        new Color(255, 255, 0), 
        new Color(0, 255, 255), 
        new Color(255, 0, 255), 
        new Color(55, 155, 255), 
    };

    private Color[] colorsDarker = { 
        Color.BLACK, 
        new Color(110, 0, 0), 
        new Color(0, 110, 0), 
        new Color(0, 0, 110), 
        new Color(110, 110, 0), 
        new Color(0, 110, 110), 
        new Color(110, 0, 110), 
        new Color(10, 50, 110), 
    };
    
    private long lastUpdateTime = System.nanoTime();
    private double da = 0;
    
    public TetrisView() throws HeadlessException {
        setSize(600, 600);
        setTitle("Radial Tetris test");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        grid = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        gridData = new int[grid.getWidth() * grid.getHeight()];
        gridData = ((DataBufferInt) grid.getRaster().getDataBuffer()).getData();
        
        offscreen = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        offscreenData = ((DataBufferInt) offscreen.getRaster().getDataBuffer()).getData();
        
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                da += 0.001;
                if (!model.isGameOver()) {
                    if (System.nanoTime() - lastUpdateTime > 400000000) {
                        model.update();
                        lastUpdateTime = System.nanoTime();
                    }
                }
                repaint();
            }
        }, 100, 10);
        
    }

    @Override
    public void paint(Graphics g) {
        drawGrid(grid.getGraphics(), 0, -20);
        
        Graphics2D osg = (Graphics2D) offscreen.getGraphics();
        drawOffscreen(osg);
        osg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        osg.drawImage(offscreen, 0, 0, offscreen.getWidth() / 2, offscreen.getHeight() / 2, 0, 0, offscreen.getWidth(), offscreen.getHeight(), null);
        
        g.drawImage(offscreen, 0, 0, getWidth(), getHeight(), 0, 0, offscreen.getWidth() / 2, offscreen.getHeight() / 2, null);
    }
    
    private void drawOffscreen(Graphics2D g) {
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, getWidth(), getHeight());
        
        for (int y=0; y<offscreen.getHeight(); y++) {
            for (int x=0; x<offscreen.getWidth(); x++) {
                Point p = radialTransform(x, y, da);
                int c = 0xFFFFFFFF;
                if (p != null) {
                    c = gridGetPixel(p.x, p.y);
                }
                offscreenSetPixel(x, y, c);
            }
        }
        drawScore(g, 20, 50);
        if (model.isGameOver()) {
            drawGameOver(g);
        }
        else {
            drawNextBlock(g, offscreen.getWidth() / 2 - 40, offscreen.getHeight() / 2 - 40);
        }
        drawCredit(g, 515, 580);
    }
    
    private Point tp = new Point();
    
    private Point radialTransform(int x, int y, double da) {
        double dMin = 80;
        double dMax = 270;
        x = x - offscreen.getWidth() / 2;
        y = y - offscreen.getHeight() / 2;
        double a = Math.atan2(y, x) + Math.PI;
        a = (a + da) % (2 * Math.PI);
        double d = Math.sqrt(x * x + y * y);
        if (d < dMin || d > dMax) {
            return null;
        }
        double tx = (grid.getWidth() - 1) * (a / (2 * Math.PI));
        double ty = (grid.getHeight() - 1) * (1 - (d - dMin) / (dMax - dMin));
        tp.setLocation(tx, ty);
        return tp;
    }
    
    private int gridGetPixel(int x, int y) {
        return gridData[x + y * grid.getWidth()];
    }
    
    private void offscreenSetPixel(int x, int y, int c) {
        offscreenData[x + y * offscreen.getWidth()] = c;
    }

    private void drawCredit(Graphics g, int x, int y) {
        g.setColor(Color.GRAY);
        g.setFont(font);
        g.drawString("by O.L.", x, y);
    }

    private void drawScore(Graphics g, int x, int y) {
        g.setColor(Color.BLACK);
        g.setFont(font);
        g.drawString("SCORE: " + model.getScore(), x, y);
    }
    
    private void drawGrid(Graphics g, int dx, int dy) {
        int cellSize = 5;
        for (int row = 4; row < model.getGridRows(); row++) {
            for (int col = 0; col < model.getGridCols(); col++) {
                int x = col * cellSize + dx;
                int y = row * cellSize + dy;
                int c = model.getGridValue(col, row);
                g.setColor(Color.GRAY);
                g.fillRect(x, y, cellSize, cellSize);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x, y, cellSize, cellSize);
                if (c > 0) {
                    g.setColor(colors[c]);
                    g.fillRect(x, y, cellSize, cellSize);
                    g.setColor(colorsDarker[c]);
                    g.drawRect(x, y, cellSize, cellSize);
                }
            }
        }
        g.setColor(Color.RED);
        g.fillRect(0, 0, 2, grid.getHeight());
        g.fillRect(grid.getWidth() - 1, 0, 2, grid.getHeight());
    }
    
    private void drawNextBlock(Graphics g, int dx, int dy) {
        g.setColor(Color.DARK_GRAY);
        g.setFont(font);
        g.drawString("NEXT: ", dx, dy);
        int cellSize = 20;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int x = col * cellSize + dx;
                int y = row * cellSize + dy + 10;
                int c = model.getNextBlockValue(col, row);
                g.setColor(Color.GRAY);
                g.drawRect(x, y, cellSize, cellSize);
                if (c > 0) {
                    g.setColor(colors[c]);
                    g.fillRect(x, y, cellSize, cellSize);
                    g.setColor(colorsDarker[c]);
                    g.drawRect(x, y, cellSize, cellSize);
                }
            }
        }
    }

    public void drawGameOver(Graphics g) {
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.fillRect(145, 250, 300, 100);
        g.setColor(getForeground());
        g.drawRect(145, 250, 300, 100);
        g.drawString("GAME OVER", 230, 290);
        g.drawString("PRESS SPACE TO PLAY", 180, 325);
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (model.isGameOver()) {
                if (e.getKeyCode() == 32) {
                    model.start();
                }
            }
            else {
                switch (e.getKeyCode()) {
                    case 37: model.move(-1); break;
                    case 39: model.move(1); break;
                    case 38: model.rotate(); break;
                    case 40: model.down(); break;
                    case 65: model.update(); break;
                }
            }
        }
        repaint();
    }
        
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TetrisView view = new TetrisView();
                view.setVisible(true);
            }
        });
    }
    
}
