<#-- ****************************************************************************** -->
<#-- ********************************** FONCTIONS ********************************* -->
<#-- ****************************************************************************** -->
<#function getCustomerBlurId customer>
    <#local customer_firstname=customer[".//attr[@name='firstname']/value"]/>
    <#local customer_lastname=customer[".//attr[@name='lastname']/value"]/>
    <#local customer_civility=customer[".//attr[@name='civility']/value"]/>
    <#local customer_phones=[ "\"" + customer[".//attr[@name='contactphone']/value"] + "\"" ]/>
    <#return Fn.query(resources, 'blurIdGenerator', '{"blur_mode":"SIGNATURE", "processId":"${PROCESS_ID}", "key":"${ANONIMYZATION_KEY}", "firstName":"${customer_firstname}", "lastName":"${customer_lastname}", "civility":"${customer_civility}", "id":"", "phones":[${customer_phones?join(",")}], "emails":[], "strategies":["CIVILITY_FIRSTNAME_LASTNAME", "CIVILITY_FIRSTNAME_LASTNAME_PHONES"]}', 'PROCESS')[0].blurId[0]>
</#function>
<#function getGender customer>
	<#assign customer_gender="male"/>
	<#assign civility=customer[".//attr[@name='civility']/value"]/>
	<#if civility?has_content && "${civility[0]}"?matches("[Mm][Ll][Ll][Ee]|[Mm][Mm][Ee]?")>
		<#assign customer_gender="female"/>
	</#if>
	<#return customer_gender>
</#function>
<#function getRandomCustomer customer>
	<#assign gender=getGender(customer)/>
	<#return Fn.query(resources, 'randomUserGenerator', '{"gender":"${gender}","count":1,"processId":"${PROCESS_ID}","reuse":"true", "blurid":"${customer_blurId}"}', 'PROCESS')>
</#function>
<#-- ****************************************************************************** -->
<#-- ****************************** MACRO DE LOGGING ****************************** -->
<#-- ****************************************************************************** -->
<#macro log level message>
	<#assign void = Fn.log(level, message)>
</#macro>
<#-- ****************************************************************************** -->
<#-- *********************** MACROS DE PARCOURS RECURSIF ************************** -->
<#-- ****************************************************************************** -->
<#macro ELEMENT level element>
	<#if element?node_type == "element">
		<#assign children=element?children>
		<#if children?size == 1 && children[0]?node_type == "text" && children[0]?trim?has_content>
			<#assign anonymized_value=children[0]/>
${""?left_pad(level * 3)}<${element?node_name}<#list element.@@ as attr> ${attr?node_name}="${attr}"</#list>>${anonymized_value}</${element?node_name}>
		<#else>
${""?left_pad(level * 3)}<${element?node_name}<#list element.@@ as attr> ${attr?node_name}="${attr}"</#list>>
			<#list element?children as c>
				<#if c?node_type == "element">
					<#if c?node_name == "attr">
						<@ATTRIBUTE level=level+1 element=c/>
					<#elseif c?node_name != "value">
						<@ELEMENT level=level+1 element=c/>
					</#if>
				<#elseif c?node_type == "text" && c?trim?has_content>
					${c}
				</#if>
			</#list>
${""?left_pad(level * 3)}</${element?node_name}>
		</#if>
	</#if>
</#macro>
<#macro ATTRIBUTE level element>
${""?left_pad(level * 3)}<${element?node_name}<#list element.@@ as attr> ${attr?node_name}="${attr}"</#list>>
	<#list element?children as child>
		<#if child?node_type == "element" && child?node_name == "value">		
			<@ANONIMYZE_VALUE level=level+1 element=element child=child/>
		</#if>
	</#list>
${""?left_pad(level * 3)}</${element?node_name}>
</#macro>
<#-- ****************************************************************************** -->
<#-- ****************************** MACROS D'ATTRIBUT ***************************** -->
<#-- ****************************************************************************** -->
<#macro ANONIMYZE_VALUE level element child>
	<#switch element.@name>
		<#case "order-code">
			<#assign anonymized_value=""/>
			<#local m = child?string?matches(r"O-[0-9]+-([0-9]+)")/>
			<#if m>
				<#local anonymizedCustomerId=Fn.query(resources, 'randomIntegerGenerator', '{"count":1,"processId":"${PROCESS_ID}","reuse":"true", "blurid":"${customer_blurId}","minValue":1,"maxValue":1000}', 'PROCESS')[0].value[0]/>               
				<#assign anonymized_value="O-${anonymizedCustomerId}-${m?groups[1]}"/>
            </#if>
			<#break>
		<#case "firstname">
			<#assign anonymized_value=randomCustomer.name_first[0]/>
			<#break>	
		<#case "lastname">
			<#assign anonymized_value=randomCustomer.name_last[0]/>
			<#break>	
		<#case "contactphone">
			<#assign anonymized_value='0' + Fn.query(resources, 'randomIntegerGenerator', '{"count":1,"processId":"${PROCESS_ID}","reuse":"true", "blurid":"${customer_blurId}","minValue":600000000,"maxValue":799999999}', 'PROCESS')[0].value[0]/>
			<#break>	
		<#default>
			<#assign anonymized_value=child/>
	</#switch>
${""?left_pad(level * 3)}<value>${anonymized_value}</value>
<#-- 
${""?left_pad(level * 3)}<${element?node_name}<#list element.@@ as attr> ${attr?node_name}="${attr}"</#list>>${anonymized_value}</${element?node_name}>
 -->
</#macro>
<#-- ****************************************************************************** -->
<#-- ***************************** DEBUT DU SCRIPT ******************************** -->
<#-- ****************************************************************************** -->
<#assign PROCESS_ID="PROCESS-ATELIER2"/>
<#assign ANONIMYZATION_KEY="ONKEY-ATELIER2"/>
<?xml version="1.0" encoding="UTF-8"?>
<orders>
<#assign orders=atelier2FileToAnonymize["/orders/order"]/>
<#list orders as order>
	<#assign customer_blurId=getCustomerBlurId(order["./customer"])/>
	<#assign randomCustomer=getRandomCustomer(order["./customer"])[0]/>
	<@ELEMENT level=1 element=order/>
</#list>
</orders>