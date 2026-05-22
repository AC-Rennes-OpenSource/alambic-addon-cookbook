<#-- ****************************************************************************** -->
<#-- ****************************** MACRO DE LOGGING ****************************** -->
<#-- ****************************************************************************** -->
<#macro LOG level message>
	<#local void = Fn.log(level, message)>
</#macro>
<#-- ****************************************************************************** -->
<#-- *************** MACRO DE MISE A JOUR D'UNE FICHE PERSONNE ******************** -->
<#-- ****************************************************************************** -->
<#macro UPDATE_STRUCTURE structure>
		<entry verifyIfExist="(&amp;(objectClass=ENTStructure)(ou=%RDN%))">
			<variables>
			        <variable name="RDN">${structure.identifiant_de_l_etablissement[0]}</variable>
			</variables>
			<dn>ou=%RDN%,%LDAP_ENT_ouStructuresDn%</dn>
			<attributes>
				<attr name="objectClass" modifyMode="ignore">
					<value>top</value>
					<value>organizationalUnit</value>
					<value>ENTOrganisation</value>
					<value>ENTStructure</value>
					<value>ENTEtablissement</value>
				</attr>
				<attr name="ou" modifyMode="replace">
					<value>${structure.identifiant_de_l_etablissement[0]}</value>
				</attr>
				<attr name="ENTDisplayName" modifyMode="replace">
					<value>${structure.nom_etablissement[0]}</value>
				</attr>
				<attr name="description" modifyMode="replace">
					<value>Une école</value>
				</attr>				
				<attr name="ENTStructureTypeStruct" modifyMode="replace">
					<value>${structure.libelle_nature[0]}</value>
				</attr>
				<attr name="ENTStructureUAI" modifyMode="replace">
					<value>${structure.identifiant_de_l_etablissement[0]}</value>
				</attr>				
				<attr name="postalCode" modifyMode="replace">
					<value>${structure.code_postal[0]}</value>
				</attr>				
				<attr name="ENTStructureEmail" modifyMode="replace">
					<value>${structure.mail[0]}</value>
				</attr>				
				<attr name="telephoneNumber" modifyMode="replace">
					<value>${structure.telephone[0]}</value>
				</attr>				
				<attr name="ENTEtablissementMinistereTutelle" modifyMode="replace">
					<value>${structure.ministere_tutelle[0]}</value>
				</attr>				
			</attributes>
		</entry>
</#macro>
<#-- ****************************************************************************** -->
<#-- ***************************** DEBUT DU SCRIPT ******************************** -->
<#-- ****************************************************************************** -->
<?xml version="1.0" encoding="UTF-8"?>
<alambic>
	<entries>
<#assign filesRSet=Fn.getEntries(resources, "fichiers-listant-les-structures", "")/>
<#if filesRSet?has_content && filesRSet?size gt 0>
	<#list filesRSet?map(file -> file.path[0])?sort as filepath>
		<@LOG level="INFO" message="Traitement du fichier '${filepath}'"/>
		<#assign fileNode=Fn.getNodeModel(filepath)/>
		<#list fileNode["/structures/structure"] as structureNode>
			<@UPDATE_STRUCTURE structure=structureNode/>
		</#list>
	</#list>
</#if>
	</entries>
</alambic>