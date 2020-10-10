import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class DemoViewer
{

	public static void main(String[] args)
	{
		JFrame frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        // slider to control horizontal rotation
        JSlider headingSlider = new JSlider(0, 360, 180);
        pane.add(headingSlider, BorderLayout.SOUTH);

        // slider to control vertical rotation
        JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -90, 90, 0);
        pane.add(pitchSlider, BorderLayout.EAST);

        // panel to display render results
        JPanel renderPanel = new JPanel() 
        {
            public void paintComponent(Graphics g) 
            {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                List<Triangle> tris = new ArrayList<>();
                tris.add(new Triangle(new Vertex(100, 100, 100),
                                      new Vertex(-100, -100, 100),
                                      new Vertex(-100, 100, -100),
                                      Color.WHITE));
                tris.add(new Triangle(new Vertex(100, 100, 100),
                                      new Vertex(-100, -100, 100),
                                      new Vertex(100, -100, -100),
                                      Color.RED));
                tris.add(new Triangle(new Vertex(-100, 100, -100),
                                      new Vertex(100, -100, -100),
                                      new Vertex(100, 100, 100),
                                      Color.GREEN));
                tris.add(new Triangle(new Vertex(-100, 100, -100),
                                      new Vertex(100, -100, -100),
                                      new Vertex(-100, -100, 100),
                                      Color.BLUE));
                
                //calculate transform matrix:
                double heading = Math.toRadians(headingSlider.getValue());
                double pitch = Math.toRadians(pitchSlider.getValue());                
                Matrix3 headingTransform = new Matrix3(new double[] {
                        Math.cos(heading), 0, Math.sin(heading),
                        0, 1, 0,
                        -Math.sin(heading), 0, Math.cos(heading)
                    });             
                Matrix3 pitchTransform = new Matrix3(new double[] {
                        1, 0, 0,
                        0, Math.cos(pitch), Math.sin(pitch),
                        0, -Math.sin(pitch), Math.cos(pitch)
                    });
                Matrix3 transform = headingTransform.multiply(pitchTransform);
                
                BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                
                double[] zBuffer = new double[img.getWidth() * img.getHeight()]; //store the max-height of the pixel in that position
		         // initialize array with extremely far away depths
		         for (int q = 0; q < zBuffer.length; q++) 
		         {
		             zBuffer[q] = Double.NEGATIVE_INFINITY;
		         }
		         
            	for (Triangle t : tris) 
            	{
            	    Vertex v1 = transform.transform(t.v1);
            	    Vertex v2 = transform.transform(t.v2);
            	    Vertex v3 = transform.transform(t.v3);

            	    // since we are not using Graphics2D anymore,
            	    // we have to do translation manually
            	    v1.x += getWidth() / 2;
            	    v1.y += getHeight() / 2;
            	    v2.x += getWidth() / 2;
            	    v2.y += getHeight() / 2;
            	    v3.x += getWidth() / 2;
            	    v3.y += getHeight() / 2;
            	    
            	    //calculate normal vector
            	    Vertex ab = new Vertex(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
                    Vertex ac = new Vertex(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);
                    Vertex norm = new Vertex(
                         ab.y * ac.z - ab.z * ac.y,
                         ab.z * ac.x - ab.x * ac.z,
                         ab.x * ac.y - ab.y * ac.x
                    );
                    double normalLength = Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z);
                    norm.x /= normalLength;
                    norm.y /= normalLength;
                    norm.z /= normalLength;
                    
                    double angleCos = Math.abs(norm.z); //cos value
            	    
            	    // compute rectangular bounds for triangle
            	    int minX = (int) Math.max(0, Math.ceil(Math.min(v1.x, Math.min(v2.x, v3.x))));
            	    int maxX = (int) Math.min(img.getWidth() - 1, 
            	                              Math.floor(Math.max(v1.x, Math.max(v2.x, v3.x))));
            	    int minY = (int) Math.max(0, Math.ceil(Math.min(v1.y, Math.min(v2.y, v3.y))));
            	    int maxY = (int) Math.min(img.getHeight() - 1,
            	                              Math.floor(Math.max(v1.y, Math.max(v2.y, v3.y))));

            	    double triangleArea = (v1.y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - v1.x);    //area of triangle X2      	                    
            	    
            	    for (int y = minY; y <= maxY; y++) 
            	    {
            	        for (int x = minX; x <= maxX; x++) 
            	        {
            	            double b1 = ((y - v3.y) * (v2.x - v3.x) + (v2.y - v3.y) * (v3.x - x)) / triangleArea;
            	            double b2 = ((y - v1.y) * (v3.x - v1.x) + (v3.y - v1.y) * (v1.x - x)) / triangleArea;
            	            double b3 = ((y - v2.y) * (v1.x - v2.x) + (v1.y - v2.y) * (v2.x - x)) / triangleArea;            	            
            	            
            	            if (b1 >= 0 && b1 <= 1 && b2 >= 0 && b2 <= 1 && b3 >= 0 && b3 <= 1)   //the pixel is in rasterized area
            	            {
            	            	double depth = b1 * v1.z + b2 * v2.z + b3 * v3.z; //pixels depth(z)          	  
            	            	int zIndex = y * img.getWidth() + x;
            	            	if (zBuffer[zIndex] < depth)  //if it is above the exist one, update it
            	            	{
            	                    img.setRGB(x, y, getShade(t.color, angleCos).getRGB());
            	                    zBuffer[zIndex] = depth;
            	                }            	                
            	            }
            	        }
            	    }
            	}

            	g2.drawImage(img, 0, 0, null);
            }
        };
        
        pane.add(renderPanel, BorderLayout.CENTER);

        frame.setSize(400, 400);
        frame.setVisible(true);
        
        headingSlider.addChangeListener(new ChangeListener() {
        	public void stateChanged(ChangeEvent e)
        	{
        		renderPanel.repaint();
        	}
        });
        pitchSlider.addChangeListener(new ChangeListener() {
        	public void stateChanged(ChangeEvent e)
        	{
        		renderPanel.repaint();
        	}
        });
        
	}
	
	/**
	 * calculate the color after shade
	 * @param color
	 * @param shade 
	 * @return
	 */
    public static Color getShade(Color color, double shade) {
        double redLinear = Math.pow(color.getRed(), 2.4) * shade;
        double greenLinear = Math.pow(color.getGreen(), 2.4) * shade;
        double blueLinear = Math.pow(color.getBlue(), 2.4) * shade;

        int red = (int) Math.pow(redLinear, 1/2.4);
        int green = (int) Math.pow(greenLinear, 1/2.4);
        int blue = (int) Math.pow(blueLinear, 1/2.4);

        return new Color(red, green, blue);
    }
}
