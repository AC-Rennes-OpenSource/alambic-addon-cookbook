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
<#macro LOG level message>
	<#assign void = Fn.log(level, message)>
</#macro>
<#-- ****************************************************************************** -->
<#-- ***************************** DEBUT DU SCRIPT ******************************** -->
<#-- ****************************************************************************** -->
<#assign resultset=xml_store[variables.xpath_query]/>
<#assign reg = "Bretagne"/>
<@compress single_line=true>
{
	"status": "success",
	"total": ${resultset?size},
	"hits": [
	<#list resultset as result>
		{
			"nom": "${result.nom[0]}",
			"code_postal": "${result.code_postal[0]}",
			"departement": "${result.departement[0]}",
			"population": ${result.population},
			"region": "${reg}"
		}<#sep>,</#sep>
	</#list>
	]
}
</@compress>