package chao.app.ami.launcher.drawer;

import android.text.TextUtils;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

import chao.app.ami.logs.LogHelper;


public class DrawerXmlParser {

    private static final String TAG = DrawerXmlParser.class.getSimpleName();

    private static final String XML_EVENT_ITEM = "item";
    private static final String XML_EVENT_GROUP = "group";
    private static final String XML_EVENT_DRAWER = "drawer";
    private static final String XML_EVENT_EXTRA = "extra";
    private static final String XML_EVENT_PROPERTY = "property";
    private static final String XML_EVENT_INPUT = "input";

    private static final String XML_ATTRIBUTE_NAME = "name";
    private static final String XML_ATTRIBUTE_PACKAGE = "packageName";
    private static final String XML_ATTRIBUTE_COMPONENT = "component";
    private static final String XML_ATTRIBUTE_FLAGS = "flags";
    private static final String XML_ATTRIBUTE_PROPERTY_KEY = "name";
    private static final String XML_ATTRIBUTE_PROPERTY_VALUE = "value";
    private static final String XML_ATTRIBUTE_EXTRA_FORMAT = "format";
    private static final String XML_ATTRIBUTE_VIEW_ID = "viewId";
    private static final String XML_ATTRIBUTE_TEXT = "text";


    private static final String XML_DEFAULT_NAME = "<unknown>";


    private XmlPullParser mPullParser;
    private DrawerXmlParserListener mXmlParserListener;

    public interface DrawerXmlParserListener {
        void onXmlParserDone(DrawerNode rootNode);

        void onXmlParserFailed(Exception e);
    }

    public void setOnDrawerXmlParserListener(DrawerXmlParserListener listener) {
        mXmlParserListener = listener;
    }

    public DrawerXmlParser() {
        mPullParser = Xml.newPullParser();
    }

    public void parseDrawer(InputStream in, DrawerXmlParserListener listener) {
        mXmlParserListener = listener;
        try {
            mPullParser.setInput(in, "utf-8");
            int eventType = mPullParser.getEventType();
            DrawerNode root = null;
            Node current = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String nodeName = mPullParser.getName();
                String name = mPullParser.getAttributeValue(null, XML_ATTRIBUTE_NAME);
                if (TextUtils.isEmpty(name)) {
                    name = XML_DEFAULT_NAME;
                }
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        switch (nodeName) {
                            case XML_EVENT_DRAWER:
                                if (XML_DEFAULT_NAME.equals(name)) {
                                    name = "/";
                                }
                                root = new DrawerNode(name);
                                String packageName = mPullParser.getAttributeValue(null, XML_ATTRIBUTE_PACKAGE);
                                root.setPackageName(packageName);
                                current = root;
                                break;
                            case XML_EVENT_ITEM:
                                ComponentNode componentNode = new ComponentNode(name);
                                String component = mPullParser.getAttributeValue(null, XML_ATTRIBUTE_COMPONENT);
                                String flags = mPullParser.getAttributeValue(null, XML_ATTRIBUTE_FLAGS);
                                if (!(current instanceof NodeGroup)) {
                                    throw new DrawerParserException("wrong format in drawer xml.");
                                }
                                componentNode.setParent((NodeGroup) current);
                                componentNode.setComponent(component);
                                componentNode.setFlags(flags);
                                current = componentNode;
                                break;
                            case XML_EVENT_GROUP:
                                NodeGroup nodeGroup = new NodeGroup(name);
                                if (!(current instanceof NodeGroup)) {
                                    throw new DrawerParserException("wrong format in drawer xml.");
                                }
                                nodeGroup.setParent((NodeGroup) current);
                                current = nodeGroup;
                                break;
                            case XML_EVENT_EXTRA:
                                if (!(current instanceof ComponentNode)) {
                                    throw new DrawerParserException("xml err: only component has extras.");
                                }

                                componentNode = (ComponentNode) current;
                                String key = mPullParser.getAttributeValue(null, XML_ATTRIBUTE_PROPERTY_KEY);
                                String value = mPullParser.getAttributeValue(null, XML_ATTRIBUTE_PROPERTY_VALUE);
                                String format = mPullParser.getAttributeValue(null, XML_ATTRIBUTE_EXTRA_FORMAT);
                                Extra extra = new Extra(key, value, format);
                                componentNode.addExtra(extra);
                                break;
                            case XML_EVENT_PROPERTY:
                                key = mPullParser.getAttributeValue(null, XML_ATTRIBUTE_PROPERTY_KEY);
                                value = mPullParser.getAttributeValue(null, XML_ATTRIBUTE_PROPERTY_VALUE);
                                Property property = new Property(key, value);
                                current.addProperty(property);
                                break;
                            case XML_EVENT_INPUT:
                                if (!(current instanceof ComponentNode)) {
                                    throw new DrawerParserException("xml err: only component has input event.");
                                }
                                componentNode = (ComponentNode) current;
                                String viewId = mPullParser.getAttributeValue(null, XML_ATTRIBUTE_VIEW_ID);
                                String text = mPullParser.getAttributeValue(null, XML_ATTRIBUTE_TEXT);
                                InputNode inputNode = new InputNode(viewId, text);
                                componentNode.addInput(inputNode);
                                break;
                            default:
                                throw new IllegalStateException("Unknown xml node name: " + nodeName);

                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (current == null) {
                            LogHelper.e(TAG, "current should not be null.");
                            break;
                        }
                        switch (nodeName) {
                            case XML_EVENT_EXTRA:
                            case XML_EVENT_PROPERTY:
                            case XML_EVENT_INPUT:
                                break;
                            default:
                                current = current.getParent();
                                break;
                        }
                        break;
                    default:
                        break;
                }
                eventType = mPullParser.next();
            }
            if (mXmlParserListener != null) {
                if (root != null) {
                    mXmlParserListener.onXmlParserDone(root);
                } else {
                    mXmlParserListener.onXmlParserFailed(null);
                }
            }

        } catch (XmlPullParserException e) {
            if (mXmlParserListener != null) {
                mXmlParserListener.onXmlParserFailed(e);
            }
            e.printStackTrace();
        } catch (IOException e) {
            if (mXmlParserListener != null) {
                mXmlParserListener.onXmlParserFailed(e);
            }
            e.printStackTrace();
        }

    }
}
