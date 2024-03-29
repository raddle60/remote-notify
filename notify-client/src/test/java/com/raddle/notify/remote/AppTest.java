package com.raddle.notify.remote;

import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.raddle.notify.remote.bean.PositionColor;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    public void testApp()
    {
    	XStream xstream = new XStream(new DomDriver());
    	xstream.alias("postion-color", ArrayList.class);
    	xstream.alias("item", PositionColor.class);
    	@SuppressWarnings("unchecked")
		ArrayList<PositionColor> list =  (ArrayList<PositionColor>) xstream.fromXML(AppTest.class.getResourceAsStream("/position-color.xml"));
    	System.out.println(list.get(0).getColor());
    }
}
