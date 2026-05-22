<#-- ****************************************************************************** -->
<#-- ********************************** FONCTIONS ********************************* -->
<#-- ****************************************************************************** -->
<#function setProgress progress status processing>
	<#assign void = activity.setProgress(progress?int)/>
	<#assign void = activity.setStatus(status)/>
	<#assign void = activity.setProcessing(processing)/>
	<#return true/>
</#function>
<#function getAttributeJsonValue person attributeName>
	<#assign jsonValue="\"\""/>
	<#if person[attributeName]?has_content>
		<#assign valuesList=[]/>
		<#if person[attributeName]?is_sequence>
			<#list person[attributeName] as attrValue>
				<#assign valuesList=valuesList + [ "\"" + attrValue?json_string?xml + "\"" ]/>
				<#assign jsonValue=valuesList?join(",")/>
			</#list>
		<#else>
			<#assign jsonValue="\"" + person[attributeName]?json_string?xml + "\""/>
		</#if>
		<#if 1 < valuesList?size>
			<#assign jsonValue="[" + jsonValue + "]"/>
		</#if>
	</#if>
	<#return jsonValue/>
</#function>
<#-- ****************************************************************************** -->
<#-- ****************************** MACRO DE LOGGING ****************************** -->
<#-- ****************************************************************************** -->
<#macro log level message>
	<#assign void = Fn.log(level, message)>
</#macro>
<#-- ****************************************************************************** -->
<#-- ***************************** DEBUT DU SCRIPT ******************************** -->
<#-- ****************************************************************************** -->
"identifiant_de_l_etablissement";"nom_etablissement";"code_postal";"nom_commune";"telephone";"mail";"ministere_tutelle";"libelle_nature"
<#assign apiRsSet=Fn.getEntries(resources, "data.education.gouv.fr")/>
<#assign apiRsSetJsonObj=Fn.getJSONObject(apiRsSet[0].item[0])/>
<#if apiRsSetJsonObj?has_content && apiRsSetJsonObj.total_count &gt; 0>
	<#list apiRsSetJsonObj.records as item>
"${item.record.fields.identifiant_de_l_etablissement}";"${item.record.fields.nom_etablissement}";"${item.record.fields.code_postal}";"${item.record.fields.nom_commune}";"${item.record.fields.telephone}";"${item.record.fields.mail}";"${item.record.fields.ministere_tutelle}";"${item.record.fields.libelle_nature}"
	</#list>
<#else>
	<@log level="WARNING" message="Résultat de recherche vide"/>
</#if>