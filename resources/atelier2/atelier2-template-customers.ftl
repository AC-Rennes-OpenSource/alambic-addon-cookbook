<#-- ****************************************************************************** -->
<#-- ********************************** FONCTIONS ********************************* -->
<#-- ****************************************************************************** -->
<#function getCustomerBlurId customer>
    <#local customer_id=customer[".//attr[@name='identifier']/value"]/>
    <#local customer_firstname=customer[".//attr[@name='firstname']/value"]/>
    <#local customer_lastname=customer[".//attr[@name='lastname']/value"]/>
    <#local customer_civility=customer[".//attr[@name='civility']/value"]/>
    <#local customer_phones=[ "\"" + customer[".//attr[@name='mobilephone']/value"] + "\"" ] + [ "\"" + customer[".//attr[@name='homephone']/value"] + "\"" ] />
    <#local customer_mails=[ "\"" + customer[".//attr[@name='mail']/value"] + "\"" ]/>
    <#return Fn.query(resources, 'blurIdGenerator', '{"blur_mode":"SIGNATURE", "processId":"${PROCESS_ID}", "key":"${ANONIMYZATION_KEY}", "firstName":"${customer_firstname}", "lastName":"${customer_lastname}", "civility":"${customer_civility}", "id":"${customer_id}", "phones":[${customer_phones?join(",")}], "emails":[${customer_mails?join(",")}], "strategies":["CIVILITY_FIRSTNAME_LASTNAME", "CIVILITY_FIRSTNAME_LASTNAME_PHONES", "CIVILITY_FIRSTNAME_LASTNAME_EMAILS"]}', 'PROCESS')[0].blurId[0]>
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
		<#case "identifier">
			<#assign anonymized_value=Fn.query(resources, 'randomIntegerGenerator', '{"count":1,"processId":"${PROCESS_ID}","reuse":"true", "blurid":"${customer_blurId}","minValue":1,"maxValue":1000}', 'PROCESS')[0].value[0]/>
			<#break>	
		<#case "firstname">
			<#assign anonymized_value=randomCustomer.name_first[0]/>
			<#break>	
		<#case "lastname">
			<#assign anonymized_value=randomCustomer.name_last[0]/>
			<#break>	
		<#case "street">
			<#assign anonymized_value=randomCustomer.location_street_name[0]/>
			<#break>	
		<#case "city">
			<#assign anonymized_value=randomCustomer.location_city[0]/>
			<#break>	
		<#case "postcode">
			<#assign anonymized_value=randomCustomer.location_postcode[0]/>
			<#break>	
		<#case "dob">
			<#local date=Fn.query(resources, 'randomDateGenerator', '{"count":1,"processId":"${PROCESS_ID}","reuse":"true", "blurid":"${customer_blurId}","lowerYear":"1950","upperYear":"2000"}', 'PROCESS')[0].timestamp[0]?number?number_to_date/>
			<#assign anonymized_value=date?string["dd/MM/yyyy"]/>
			<#break>	
		<#case "mobilephone">
			<#assign anonymized_value='0' + Fn.query(resources, 'randomIntegerGenerator', '{"count":1,"processId":"${PROCESS_ID}","reuse":"true", "blurid":"${customer_blurId}","minValue":600000000,"maxValue":799999999}', 'PROCESS')[0].value[0]/>
			<#break>	
		<#case "homephone">
			<#assign anonymized_value='0' + Fn.query(resources, 'randomIntegerGenerator', '{"count":1,"processId":"${PROCESS_ID}","reuse":"true", "blurid":"${customer_blurId}","minValue":299000000,"maxValue":299999999}', 'PROCESS')[0].value[0]/>
			<#break>	
		<#case "mail">
			<#assign anonymized_value=Fn.query(resources, 'randomMailGenerator', '{"count":1,"processId":"PROCESS_ID","reuse":"true","blurid":"${customer_blurId}","firstName":"${randomCustomer.name_first[0]}","lastName":"${randomCustomer.name_last[0]}","domain":"noreply.phm.education.gouv.fr"}', 'PROCESS')[0].mail[0]/>
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
<customers>
<#assign customers=atelier2FileToAnonymize["/customers/customer"]/>
<#list customers as customer>
	<#assign customer_blurId=getCustomerBlurId(customer)/>
	<#assign randomCustomer=getRandomCustomer(customer)[0]/>
	<@ELEMENT level=1 element=customer/>
</#list>
</customers>