package gov.pnnl.jac.geom.dmap;

import java.awt.geom.Point2D;

/**
 * DensityMap2D is an interface defining a 2-dimensional grid of
 * integer densities, with each grid cell having the same width and 
 * the same height.
 * 
 * @author d3j923
 *
 */
public interface DensityMap2D {

    /**
     * Get the number of grid units in the X-dimension. 
     * This is a width of the entire map in units.
     * @return
     */
    public int getGridLengthX();
    
    /**
     * Get the number of grid units in the Y-dimension.
     * This is the height of the map in units.
     * @return
     */
    public int getGridLengthY();
    
    /**
     * Get the width of a single unit.
     * @return
     */
    public float getUnitWidth();
    
    /**
     * Get the height of a single unit.
     * @return
     */
    public float getUnitHeight();
    
    /**
     * Get the origin x-coordinate for the map.
     * @return
     */
    public float getOriginX();
    
    /**
     * Get the origin y-coordinate for the map.
     * @return
     */
    public float getOriginY();
    
    /**
     * Get the x index of the unit containing the specified point.
     * @param xpos
     * @return
     */
    public int getXIndex(Point2D.Float point);
    
    /**
     * Get the y index of the unit containing the specified point.
     * @param point
     * @return
     */
    public int getYIndex(Point2D.Float point);
    
    /**
     * Get the density level for the unit having the specified 
     * location.
     * @param x
     * @param y
     * @return
     */
    public int getDensity(int x, int y);
    
    /**
     * Get the minimum density found in the map.
     * @return
     */
    public int getMinDensity();
    
    /**
     * Get the maximum density found in the map.
     * @return
     */
    public int getMaxDensity();
}
