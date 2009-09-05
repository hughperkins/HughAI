// Copyright Hugh Perkins 2006, 2009
// hughperkins@gmail.com http://manageddreams.com
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
// or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
//  more details.
//
// You should have received a copy of the GNU General Public License along
// with this program in the file licence.txt; if not, write to the
// Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-
// 1307 USA
// You can find the licence also on the web at:
// http://www.opensource.org/licenses/gpl-license.php
//
// ======================================================================================
//

package hughai.utils;

import org.w3c.dom.*;
import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.xpath.*;
import java.util.*;

public class XmlHelper
{
	public static Document CreateDom()
	{
		Document newdoc = getDocumentBuilder().newDocument();
		//newdoc..PreserveWhitespace = true;
		Element childnode = newdoc.createElement( "root" );
		newdoc.appendChild( childnode );
		return newdoc;
	}
	static DocumentBuilder getDocumentBuilder(){
		DocumentBuilder documentBuilder = null;
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilder = documentBuilderFactory.newDocumentBuilder();			
		} catch( Exception e ) {
			throw new RuntimeException( e );
		}
		return documentBuilder;
	}

	public static Document OpenDom( String sfilepath )
	{
		Document newdoc = null;
		try {
			newdoc = getDocumentBuilder().parse(new File(sfilepath));
		} catch( Exception e ){
			throw new RuntimeException(e);
		}
		return newdoc;
	}
	public static void SaveDom( Document dom, String filepath ) {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			DOMSource domSource = new DOMSource(dom);
			
			StreamResult streamResult = new StreamResult(new FileOutputStream(filepath));
			transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
 	        transformer.setOutputProperty( OutputKeys.ENCODING, "utf-8" );
			transformer.transform( domSource, streamResult );
		} catch( Exception e ) {
			throw new RuntimeException( e );
		}
	}
	public static Element AddChild( Element parent, String nodename )
	{
		Element childnode = parent.getOwnerDocument().createElement( nodename );
		parent.appendChild( childnode );
		return childnode;
	}
	public static List<Element> SelectElements( Element parent, String expression ) {
		List<Element> nodes = new ArrayList<Element>();
		XPath xpath = getXPath();
		try{
			XPathExpression xpathExpression = xpath.compile(expression);
			NodeList nodeList = (NodeList) xpathExpression.evaluate(parent, XPathConstants.NODESET);
			for( int i = 0; i < nodeList.getLength(); i++ ) {
				nodes.add( (Element)(nodeList.item(i)));
			}
		} catch ( Exception e ){
			throw new RuntimeException( e );
		}
		return nodes;
	}
	public static Element SelectSingleElement( Element parent, String expression ) {
		Element element = null;
		XPath xpath = getXPath();
		try{
			XPathExpression xpathExpression = xpath.compile(expression);
			element = (Element) xpathExpression.evaluate(parent, XPathConstants.NODE);
		} catch ( Exception e ){
			throw new RuntimeException( e );
		}
		return element;
	}
	public static XPath getXPath(){
		return getXPathFactory().newXPath();
	}
	public static XPathFactory getXPathFactory(){
		return XPathFactory.newInstance();
	}
}
