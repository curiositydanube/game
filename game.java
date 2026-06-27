
/**
 * game
 *
 * @CuriosityDanube
 * @game V1
 */
import javax.swing.JFrame;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class game extends Canvas implements Runnable, KeyListener {
    private static final long serialVersionUID = 1L;

    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;
    private Thread thread;
    private boolean running = false;
    private BufferedImage image;
    private int[] pixels;
    private static final int MAP_SIZE = 16;
    private static int[] map = new int[MAP_SIZE * MAP_SIZE];
    private double posX = 3.5, posY = 3.5; 
    private double dirX = 1.0, dirY = 0.0; 
    private double planeX = 0.0, planeY = 0.66; 
    private boolean moveForward, moveBackward, turnLeft, turnRight;
    private boolean placeBlock, breakBlock;

    public game() {
        Dimension size = new Dimension(WIDTH, HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        pixels = ((java.awt.image.DataBufferInt) image.getRaster().getDataBuffer()).getData();
        addKeyListener(this);
        generateClassicMap();
    }

    private void generateClassicMap() {
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                if (x == 0 || x == MAP_SIZE - 1 || y == 0 || y == MAP_SIZE - 1) {
                    map[x + y * MAP_SIZE] = 1; 
                } else if ((x == 5 && y == 5) || (x == 10 && y == 8)) {
                    map[x + y * MAP_SIZE] = 2; 
                } else {
                    map[x + y * MAP_SIZE] = 0; 
                }
            }
        }
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        final double ns = 1000000000.0 / 60.0; 
        double delta = 0;
        
        requestFocus();
        
        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            
            while (delta >= 1) {
                update();
                delta--;
            }
            render();
        }
    }

    private void update() {
        double moveSpeed = 0.08;
        double rotSpeed = 0.04;
        
        if (moveForward) {
            if (map[(int)(posX + dirX * moveSpeed) + (int)posY * MAP_SIZE] == 0) posX += dirX * moveSpeed;
            if (map[(int)posX + (int)(posY + dirY * moveSpeed) * MAP_SIZE] == 0) posY += dirY * moveSpeed;
        }
        if (moveBackward) {
            if (map[(int)(posX - dirX * moveSpeed) + (int)posY * MAP_SIZE] == 0) posX -= dirX * moveSpeed;
            if (map[(int)posX + (int)(posY - dirY * moveSpeed) * MAP_SIZE] == 0) posY -= dirY * moveSpeed;
        }
        
        if (turnRight) {
            double oldDirX = dirX;
            dirX = dirX * Math.cos(-rotSpeed) - dirY * Math.sin(-rotSpeed);
            dirY = oldDirX * Math.sin(-rotSpeed) + dirY * Math.cos(-rotSpeed);
            double oldPlaneX = planeX;
            planeX = planeX * Math.cos(-rotSpeed) - planeY * Math.sin(-rotSpeed);
            planeY = oldPlaneX * Math.sin(-rotSpeed) + planeY * Math.cos(-rotSpeed);
        }
        if (turnLeft) {
            double oldDirX = dirX;
            dirX = dirX * Math.cos(rotSpeed) - dirY * Math.sin(rotSpeed);
            dirY = oldDirX * Math.sin(rotSpeed) + dirY * Math.cos(rotSpeed);
            double oldPlaneX = planeX;
            planeX = planeX * Math.cos(rotSpeed) - planeY * Math.sin(rotSpeed);
            planeY = oldPlaneX * Math.sin(rotSpeed) + planeY * Math.cos(rotSpeed);
        }
        
        int targetX = (int)(posX + dirX * 1.2);
        int targetY = (int)(posY + dirY * 1.2);
        
        if (targetX > 0 && targetX < MAP_SIZE - 1 && targetY > 0 && targetY < MAP_SIZE - 1) {
            if (placeBlock && map[targetX + targetY * MAP_SIZE] == 0) {
                map[targetX + targetY * MAP_SIZE] = 2; 
                placeBlock = false;
            }
            if (breakBlock && map[targetX + targetY * MAP_SIZE] != 0) {
                map[targetX + targetY * MAP_SIZE] = 0; 
                breakBlock = false;
            }
        }
    }

    private void render() {
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(3);
            return;
        }
        
        for (int i = 0; i < pixels.length; i++) {
            if (i < pixels.length / 2) {
                pixels[i] = 0x99CCFF; 
            } else {
                pixels[i] = 0x555555; 
            }
        }
        
        for (int x = 0; x < WIDTH; x++) {
            double cameraX = 2 * x / (double) WIDTH - 1;
            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;
            
            int mapX = (int) posX;
            int mapY = (int) posY;
            
            double sideDistX, sideDistY;
            
            double deltaDistX = (rayDirX == 0) ? Double.MAX_VALUE : Math.abs(1 / rayDirX);
            double deltaDistY = (rayDirY == 0) ? Double.MAX_VALUE : Math.abs(1 / rayDirY);
            double perpWallDist;
            
            int stepX, stepY;
            int hit = 0;
            int side = 0; 
            
            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (posX - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - posX) * deltaDistX;
            }
            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (posY - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - posY) * deltaDistY;
            }
            
            while (hit == 0) {
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }
                if (mapX < 0 || mapX >= MAP_SIZE || mapY < 0 || mapY >= MAP_SIZE) break;
                if (map[mapX + mapY * MAP_SIZE] > 0) hit = 1;
            }
            
            if (hit == 1) {
                if (side == 0) perpWallDist = (sideDistX - deltaDistX);
                else perpWallDist = (sideDistY - deltaDistY);
                
                int lineHeight = (int) (HEIGHT / perpWallDist);
                
                int drawStart = -lineHeight / 2 + HEIGHT / 2;
                if (drawStart < 0) drawStart = 0;
                int drawEnd = lineHeight / 2 + HEIGHT / 2;
                if (drawEnd >= HEIGHT) drawEnd = HEIGHT - 1;
                
                int color = 0x00FF00; 
                int blockType = map[mapX + mapY * MAP_SIZE];
                if (blockType == 1) color = 0x777777; 
                
                if (side == 1) {
                    color = (color >> 1) & 0x7F7F7F; // Fixed the missing 0x hex prefix here
                }
                
                for (int y = drawStart; y < drawEnd; y++) {
                    pixels[x + y * WIDTH] = color;
                }
            }
        }
        
        Graphics g = bs.getDrawGraphics();
        g.drawImage(image, 0, 0, WIDTH, HEIGHT, null);
        g.setColor(Color.WHITE);
        g.drawLine(WIDTH / 2 - 8, HEIGHT / 2, WIDTH / 2 + 8, HEIGHT / 2);
        g.drawLine(WIDTH / 2, HEIGHT / 2 - 8, WIDTH / 2, HEIGHT / 2 + 8);
        g.drawString("game by CuriosityDanube", 15, 25);
        g.drawString("WASD / Arrows to Move | R to Build | F to Break", 15, 45);
        g.dispose();
        bs.show();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) moveForward = true;
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) moveBackward = true;
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) turnLeft = true;
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) turnRight = true;
        if (code == KeyEvent.VK_R) placeBlock = true;
        if (code == KeyEvent.VK_F) breakBlock = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) moveForward = false;
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) moveBackward = false;
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) turnLeft = false;
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) turnRight = false;
        if (code == KeyEvent.VK_R) placeBlock = false;
        if (code == KeyEvent.VK_F) breakBlock = false;
    }

    @Override 
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        game game = new game();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setTitle("game");
        frame.setResizable(false);
        frame.setVisible(true);
        
        game.start();
    }
}