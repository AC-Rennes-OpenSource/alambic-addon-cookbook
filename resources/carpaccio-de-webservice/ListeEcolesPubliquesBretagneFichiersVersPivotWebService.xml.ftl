<#-- ****************************************************************************** -->
<#-- ****************************** MACRO DE LOGGING ****************************** -->
<#-- ****************************************************************************** -->
<#macro LOG level message>
	<#local void = Fn.log(level, message)>
</#macro>
<#-- ******************************************************************* -->
<#-- *************** MACRO DE PAYLOAD D'UNE REQUETE ******************** -->
<#-- ******************************************************************* -->
<#macro PAYLOAD structure>
<@compress single_line=true>
{
	"uai":"${structure.identifiant_de_l_etablissement[0]}",
	"nom":"${structure.nom_etablissement[0]}",
	"type":"${structure.libelle_nature[0]}",
	"ministere_tutelle":"${structure.ministere_tutelle[0]}",
	"code_postal":"${structure.code_postal[0]}",
	"telephone":"${structure.telephone[0]}",
	"courriel":"${structure.mail[0]}"
}
</@compress>
</#macro>
<#-- ****************************************************************************** -->
<#-- ***************************** DEBUT DU SCRIPT ******************************** -->
<#-- ****************************************************************************** -->
<?xml version="1.0" encoding="UTF-8"?>
<requests>
<#assign filesRSet=Fn.getEntries(resources, "fichiers-listant-les-structures", "")/>
<#if filesRSet?has_content && filesRSet?size gt 0>
	<#list filesRSet?map(file -> file.path[0])?sort as filepath>
		<@LOG level="INFO" message="Traitement du fichier '${filepath}'"/>
		<#assign fileNode=Fn.getNodeModel(filepath)/>
		<#list fileNode["/structures/structure"] as structureNode>
	<request uri="%WEB_SERVICE_URI_%ALAMBIC_TARGET_ENVIRONMENT%%" method="POST">
		<headers>
			<header name="Content-Type">application/json; charset=UTF-8</header>
		</headers>
		<response_codes>
			<code type="success">200</code>
		</response_codes>
		<payload><@PAYLOAD structure=structureNode/></payload>
	</request>
		</#list>
	</#list>
</#if>
</requests>