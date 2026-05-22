<#ftl output_format="XML">
<#-- ****************************************************************************** -->
<#-- ********************************** FONCTIONS ********************************* -->
<#-- ****************************************************************************** -->
<#function setProgress progress status processing>
	<#assign void = activity.setProgress(progress?int)/>
	<#assign void = activity.setStatus(status)/>
	<#assign void = activity.setProcessing(processing)/>
	<#return true/>
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
<structures>
<#assign apiRsSet=Fn.getEntries(resources, "data.education.gouv.fr")/>
<#assign apiRsSetJsonObj=Fn.getJSONObject(apiRsSet[0].item[0])/>
<#if apiRsSetJsonObj?has_content && apiRsSetJsonObj.total_count &gt; 0>
	<#list apiRsSetJsonObj.records as item>
	<structure>
		<#list item.record.fields?keys as key>
		<${key}>${item.record.fields[key]}</${key}>
		</#list>
	</structure>
	</#list>
<#else>
	<@log level="WARNING" message="Résultat de recherche vide"/>
</#if>
</structures>