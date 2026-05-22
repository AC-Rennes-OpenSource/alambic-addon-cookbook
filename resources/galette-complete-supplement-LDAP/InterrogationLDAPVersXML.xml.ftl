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
<personnes>
<#assign ldapRsSet=Fn.getEntries(resources, 'Mon LDAP favori')/>
<#if ldapRsSet?has_content && ldapRsSet?size gt 0>
	<#list ldapRsSet as entry>
	<personne>
		<#list entry?keys as attribute>
			<#if entry[attribute]?is_sequence>
		<${attribute}>
				<#list entry[attribute] as item>
			<value>${item}</value>
				</#list>
		</${attribute}>
			<#else>
		<${attribute}>
			<value>${entry[attribute]}</value>
		</${attribute}>
			</#if>
		</#list>
	</personne>
	</#list>
<#else>
	<@log level="WARNING" message="Résultat de recherche vide"/>
</#if>
</personnes>