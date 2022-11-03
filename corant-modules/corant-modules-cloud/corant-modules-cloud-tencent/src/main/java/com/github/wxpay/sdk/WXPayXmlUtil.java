/*
 * <p> All code base from this package(com.github.wxpay.sdk) are copy from <a href=
 * "https://pay.weixin.qq.com/wiki/doc/api/download/WxPayAPI_JAVA.zip">pay.weixin.qq.com</a>, the
 * original source code version is 3.0.9; all the source code below this
 * package(com.github.wxpay.sdk) are belongs to the original publisher, if there is infringement,
 * please inform me(finesoft@gmail.com).
 *
 * <b>Notices:</b> This package(com.github.wxpay.sdk) is only used for learning or reference, not in
 * the production environment; If you use the package in a production environment or redistribute
 * it, any problems arising therefrom are irrelevant to us, we do not assume any legal
 * responsibility.
 */
package com.github.wxpay.sdk;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;

/**
 * 2018/7/3
 */
public final class WXPayXmlUtil {
  public static Document newDocument() throws ParserConfigurationException {
    return newDocumentBuilder().newDocument();
  }

  public static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities",
        false);
    documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities",
        false);
    documentBuilderFactory
        .setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    documentBuilderFactory.setXIncludeAware(false);
    documentBuilderFactory.setExpandEntityReferences(false);

    return documentBuilderFactory.newDocumentBuilder();
  }
}
