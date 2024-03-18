//Assets
// - MiamiTransparent.png
// - steeringWheelSmall.png
// - cockpit.png
// - trackchessboard.png
// - perspectiveTrack.png

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import java.util.Vector;
import java.util.Random;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;

import javax.imageio.ImageIO;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.Polygon;
import java.awt.Color;

public class FormulaOne {
    public FormulaOne() {
        setup();
    }

    public static void setup() {
        XOFFSET = 0;
        YOFFSET = 40;
        WINWIDTH = 500;
        WINHEIGHT = 500;

        pi = 3.14159265358979;
        quarterPi = 0.25 * pi;
        halfPi = 0.5 * pi;
        threequartersPi = 0.75 * pi;
        fivequartersPi = 1.25 * pi;
        threehalvesPi = 1.5 * pi;
        sevenquartersPi = 1.75 * pi;
        twoPi = 2.0 * pi;

        endgame = false;
        p1width = 228;
        p1height = 228;
        cockpitShift = 350;
        p1origionalX = (double) XOFFSET + ((double) WINWIDTH / 2.0) - (p1width / 2.0);
        p1origionalY = (double) YOFFSET + (double) cockpitShift;

        trackMatrix = new Vector<Vector<Vector<Integer>>>();

        try {
            background = ImageIO.read(new File("MiamiTransparent.png"));
            player = ImageIO.read(new File("steeringWheelSmall.png"));
            cockpit = ImageIO.read(new File("cockpit.png"));
            track = ImageIO.read(new File("trackchessboard.png"));
            perspectiveTrack = convertToARGB(ImageIO.read(new File("perspectiveTrack.png")))


        } catch (IOException e) {
        }
    }

    private static BufferedImage convertToARGB(BufferedImage input) {
        BufferedImage ret = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = ret.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();
        return ret;
    }

    private static class Animate implements Runnable {
        public void run() {
            while (endgame == false) {
                backgroundDraw();
                trackDraw();
                playerDraw();

                try {
                    Thread.sleep(32);
                } catch (InterruptedException e) {

                }
            }
        }
    }

    private static class PlayerMover implements Runnable {
        public PlayerMover() {
            velocitystep = 0.01;
            rotatestep = 0.023;
        }

        public void run() {
            while (endgame == false) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {

                }
                if (upPressed) {
                    p1velocity = p1velocity + velocitystep;
                }
                if (downPressed) {
                    p1velocity = p1velocity - velocitystep;
                }
                if (leftPressed) {
                    if (p1velocity < 0) {
                        p1.rotate(-rotatestep);
                    } else {
                        p1.rotate(rotatestep);
                    }
                }
                if (rightPressed) {
                    if (p1velocity < 0) {
                        p1.rotate(rotatestep);
                    } else {
                        p1.roatate(-rotatestep);
                    }
                }
            }
        }

        private double velocitystep;
        private double rotatestep;
    }

    private static int constrainToCap(int position, int differential, int cap) {
        int ret = differential;
        while (position + ret < 0) {
            ret = ret + cap;
        }
        while (position + ret >= cap) {
            ret = ret - cap;
        }
        return ret;
    }

    private static class CameraMover implements Runnable {
        public CameraMover() {

        }

        public void run() {
            while (endgame == false) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {

                }

                int sumx = (int) (-p1velocity * Math.cos(p1.getAngle() - pi / 2.0) + 0.5);
                int sumy = (int) (p1velocity * Math.sin(p1.getAngle() - pi / 2.0) + 0.5);

                camerax = camerax + constrainToCap(camerax, sumx, trackMatrix.elementAt(0).size());
                cameray = cameray + constrainToCap(cameray, sumy, trackMatrix.size());
            }
        }
    }

    private static Vector<Vector<Vector<Integer>>> splitColors(BufferedImage input) {
        Vector<Vector<Vector<Integer>>> ret = new Vector<Vector<Vector<Integer>>>();
        for (int i = 0; i < input.getWidth(); i++) {
            Vector<Vector<Integer>> tempRow = new Vector<Vector<Integer>>();

            for (int j = 0; j < input.getHeight(); j++) {
                Vector<Integer> temp = new Vector<Integer>();
                int rgb = input.getRGB(i, j);
                int r = (rgb >> 16) & 0x000000FF;
                int g = (rgb >> 8) & 0x000000FF;
                int b = rgb & 0x000000FF;

                temp.addElement(r);
                temp.addElement(g);
                temp.addElement(b);
                tempRow.addElement(temp);
            }
            ret.addElement(tempRow);
        }
        return ret;
    }

    private static void setupTrack() {
        trackMatrix = splitColors(track);
    }

    private static AffineTransformOp rotateImageObject(ImageIO obj) {
        AffineTransform at = AffineTransform.getRotateInstance(-obj.getAngle(), obj.getWidth() / 2.0, obj.getheight() / 2.0);
        AffineTransformOp atop = new AffineTransformOp((at, AffineTransformOp.TYPE_BILINEAR));
        return atop;
    }

    private static AffineTransformOp spinImageObject(ImageObject obj) {
        AffineTransform at = AffineTransform.getRotateInstance(-obj.getInternalAngle(), obj.getWidth() / 2.0, obj.getHeight() / 2.0);
        AffineTransformOp atop = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        return atop;
    }

    private static void backgroundDraw() {
        Graphics g = appFrame.getGraphics();
        Graphics2D g2D = (Graphics2D) g;
        int xshift = XOFFSET + (int) ((p1.getAngle() / twoPi) * (background.getWidth()) + 0.5);
        g2D.drawImage(background, xshift, YOFFSET, null);
        g2D.drawImage(background, xshift - background.getWidth(), YOFFSET,
                null);
        g2D.drawImage(cockpit, XOFFSET, cockpitShift, null);
        g2D.drawImage(rotateImageObject(p1).filter(player, null), (int) (p1.getX() + 0.5), (int) (p1.getY() + 0.5), null);
    }

    private static Vector<Vector<Vector<Integer>>> perspectiveFromRectangle(Vector<Vector<Vector<Integer>>> inputGrid, int base) {
        Vector<Vector<Vector<Integer>>> ret = new Vector<Vector<Vector<Integer>>>();
// allocate space for ret
        for (int i = 0; i < inputGrid.size(); i++) {
            Vector<Vector<Integer>> tempRow = new Vector<Vector<Integer>>();
            for (int j = 0; j < inputGrid.elementAt(i).size(); j++) {
                Vector<Integer> tempRGB = new Vector<Integer>();
                tempRGB.addElement(0);
                tempRGB.addElement(0);
                tempRGB.addElement(0);

                tempRow.addElement(tempRGB);
            }
            ret.addElement(tempRow);
        }
        //Collapse rows from inputGrid into ret
        for (int i = 0; i < inputGrid.size(); i++) {
            for (int j = 0; j < inputGrid.elementAt(i).size(); j++) {
                double xdim = (double) inputGrid.elementAt(i).size();
                double ydim = (double) inputGrid.size();
                double width = xdim - ((double) i / (ydim - 1.0)) * (xdim - (double) base);
                double stepsize = width / xdim;
                double offset = (xdim - width) / 2.0;
                int indexi = i;
                int indexj = (int) (0.5 + offset + (double) j * stepsize);
                //System.out.println( ”i: ” + indexi + ”, j: ” + indexj );
                ret.elementAt(i).elementAt(j).set(0, inputGrid.elementAt(indexi).elementAt(indexj).elementAt(0));
                ret.elementAt(i).elementAt(j).set(1, inputGrid.elementAt(indexi).elementAt(indexj).elementAt(1));
                ret.elementAt(i).elementAt(j).set(2, inputGrid.elementAt(indexi).elementAt(indexj).elementAt(2));
            }
        }
        return ret;
    }

    private static Vector< Vector< Vector< Integer > > > rotateImage ( Vector < Vector< Vector< Integer > > > inputImg , double angle , double xpos , double ypos , boolean repeatImg ) {
        Vector< Vector< Vector< Integer > > > ret = new Vector< Vector< Vector< Integer > > >() ;
        for( int i = 0; i < inputImg.size(); i++ ) {
            Vector<Vector<Integer>> tempRow = new Vector<Vector<Integer>>();
            for (int j = 0; j < inputImg.elementAt(i).size(); j++) {
                Vector<Integer> tempPixel = new Vector<Integer>();
                for (int k = 0; k < inputImg.elementAt(i).elementAt(j).size();
                     k++) {
                    tempPixel.addElement(0);
                }
                tempRow.addElement(tempPixel);
            }
            ret.addElement(tempRow);
        }
        for(int i = 0; i < inputImg.size(); i++){
            for(int j = 0; j < inputImg.elementAt(i).size(); j++){
                int newj = (int)(0.5 + xpos + ((double)j - xpos) * Math.cos(angle) - ((double)i - ypos) * Math.sin(angle));
                int newi = (int)(0.5 + ypos + ((double)j - xpos) * Math.sin(angle) + ((double)i - ypos) * Math.cos(angle));

                if(repeatImg){
                    while (newj >= ret.elementAt())
                }
            }
        }



    public static void main(String[] args){
        setup();
        appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        appFreame.setSize(501,585);

        JPanel myPanel = new JPanel();

        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(new StartGame());
        myPanel.add(newGameButton);

        JButton quitButton = new JButton("Quit Game");
        quitButton.addActionListener(new QuitGame());
        myPanel.add(quitButton);

        bindKey (myPanel, "UP");
        bindKey (myPanel, "DOWN");
        bindKey (myPanel, "LEFT");
        bindKey (myPanel, "RIGHT");
        bindKey (myPanel, "F");

        appFrame.getContentPane().add(myPanel, "South");
        appFrame.setVisible(true);
    }

    private static Boolean endgame;
    private static BufferedImage background;
    private static BufferedImage player;
    private static BufferedImage cockpit;
    private static BufferedImage track;
    private static BufferedImage perspectiveTrack;
    private static Vector<Vector<Vector<Integer>>> trackMatrix;

    private static int camerax;
    private static int cameray;

    private static int cockpitShift;

    private static Boolean upPressed;
    private static Boolean downPressed;
    private static Boolean leftPressed;
    private static Boolean rightPressed;

    private static ImageObject p1;
    private static double p1width;
    private static double p1height;
    private static double p1origionalX;
    private static double p1origionalY;
    private static double p1velocity;

    private static int XOFFSET;
    private static int YOFFSET;
    private static int WINWIDTH;
    private static int WINHEIGHT;

    private static double pi;
    private static double quarterPi;
    private static double halfPi;
    private static double threequartersPi;
    private static double fivequartersPi;
    private static double threehalvesPi;
    private static double sevenquartersPi;
    private static double twoPi;

    private static JFrame appFrame;

    private static final int IFW = JComponent.WHEN_IN_FOCUSED_WINDOW;



}

