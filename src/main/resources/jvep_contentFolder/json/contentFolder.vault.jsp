<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="json" uri="http://www.atg.com/taglibs/json" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="prop" type="org.jahia.services.content.JCRPropertyWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%-- You can use a depthLimit query parameter to set a different depth limit (default = 1) --%>
<%-- You can use a escapeColom query parameter to activate or deactivate JCR property name escaping (replacing colon with underscore --%>
<c:set target="${renderContext}" property="contentType" value="application/json;charset=UTF-8"/>
<c:set var="depthLimit" value="${functions:default(param.depthLimit, 1)}" />
<c:set var="depthLimit" value="${functions:default(currentResource.moduleParams.depthLimit, depthLimit)}"/>
<c:set var="escapeColon" value="${functions:default(param.escapeColon, false)}" />
<json:object escapeXml="false" prettyPrint="true">
    <c:forEach items="${currentNode.properties}" var="prop">
        <c:set var="propName" value="${prop.name}"/>
        <c:if test="${escapeColon}">
            <c:set var="propName" value="${fn:replace(prop.name, ':', '_')}"/>
        </c:if>
        <c:choose>
            <c:when test="${prop.definition.hidden}"><%-- do nothing --%></c:when>
            <c:when test="${not prop.definition.multiple}">
                <json:property name="${propName}" value="${prop.string}"/>
            </c:when>
            <c:otherwise>
                <json:array name="${propName}" items="${prop.values}" var="propValue">${propValue.string}</json:array>
            </c:otherwise>
        </c:choose>
    </c:forEach>
    <c:if test="${jcr:isNodeType(currentNode, 'mix:title')}">
        <jcr:nodeProperty name="jcr:title" node="${currentNode}" var="title"/>
    </c:if>
    <json:property name="text" value="${not empty title ? title.string : currentNode.name}"/>
    <json:property name="hasChildren" value="${currentNode.nodes.size > 0}"/>
    <c:if test="${depthLimit > 0}">
        <c:if test="${currentNode.nodes.size > 0}">
            <json:array name="childNodes">
                <c:forEach items="${currentNode.nodes}" var="child">
                    <template:module node="${child}" templateType="json" editable="false" view="vault">
                        <template:param name="depthLimit" value="${depthLimit -1 }" />
                    </template:module>
                </c:forEach>
            </json:array>
        </c:if>
    </c:if>
</json:object>