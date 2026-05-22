<#-- ****************************************************************************** -->
<#-- ********************************** FONCTIONS ********************************* -->
<#-- ****************************************************************************** -->
<#function getRandomInteger>
    <#return Fn.query(resources, 'randomIntegerGenerator', '{"count":1,"minValue":100000, "maxValue":999999, "processId":"PROCESS_TESTU","reuse":"true", "blurid":"${userBlurId}"}', 'NONE')[0].value[0]/>
</#function>
<#function getRandomUser element>
	<#assign gender=getGender(element)/>
	<#return Fn.query(resources, 'randomUserGenerator', '{"gender":"${gender}","count":1,"processId":"PROCESS_TESTU","reuse":"true", "blurid":"${userBlurId}"}', "NONE")>
</#function>
<#function getGender element>
	<#assign element_gender="male"/>
	<#assign title=element["genre"]/>
	<#if title?has_content && "${title[0]}"?matches("[Mm][Ll][Ll][Ee]|[Mm][Mm][Ee]?")>
		<#assign element_gender="female"/>
	</#if>
	<#return element_gender>
</#function>
<#function getRandomDOB element>
    <#assign default_date="28/04/1973"?date["dd/MM/yyyy"]/> <#-- default value as specified within Redmine #10698 -->
    <#assign default_timestamp=default_date?long?string["0"]/>
    <#assign element_randomDOB=[{ "timestamp": ["${default_timestamp}"]}]/>
    <#assign dob=element["./DateDeNaissance"]/>
    <#if dob?has_content && dob[0]?string?has_content>
        <#assign m = dob[0]?matches(r"\d+/\d+/(\d{4})")>
        <#if m>
            <#assign yob=m?groups[1]/>
            <#assign element_randomDOB=Fn.query(resources, 'randomDateGenerator', '{"count":1,"processId":"PROCESS_TESTU","reuse":"true", "blurid":"${userBlurId}","lowerYear":"${yob}","upperYear":"${yob}"}', 'NONE')/>
            <#assign date=element_randomDOB[0].timestamp[0]?number?number_to_date/>
            <#assign element_randomDOB=date?string["dd/MM/yyyy"]/>
        <#else>
            <@log level="WARN" message="The date of birth '${dob[0]}' doesn't fit the pattern 'dd/MM/YYYY'. Arbitrary date is set."/>
        </#if>
    <#else>
        <#assign element_randomDOB=[]/>
    </#if>
    <#return element_randomDOB>
</#function>
<#function getRandomMail>
	<#local firstName=randomUserFirstName?lower_case/>
	<#local lastName=randomUserLastName?lower_case/>
	<#return Fn.query(resources, 'randomMailGenerator', '{"count":1,"processId":"PROCESS_TESTU","reuse":"true","blurid":"${userBlurId}","firstName":"${firstName}","lastName":"${lastName}","domain":"noreply.phm.education.gouv.fr"}', 'NONE')[0].mail[0]/>
</#function>
<#function getRandomUAI>
    <#return Fn.query(resources, 'randomUAIGenerator', '{"count":1,"processId":"PROCESS_TESTU","reuse":"true","blurid":"${userBlurId}","root":"001"}', 'NONE')[0].uai[0]/>
</#function>
<#function getRandomUid>
    <#return Fn.query(resources, 'randomUidGenerator', '{"count":1,"format":"LONG","processId":"PROCESS_TESTU","reuse":"true", "firstName":"${randomUserFirstName}", "lastName":"${randomUserLastName}", "blurid":"${userBlurId}"}', 'NONE')[0].uid[0]/>
</#function>
<#function getRandomUUid>
    <#return Fn.query(resources, 'randomUUidGenerator', '{"count":1, "processId":"PROCESS_TESTU","reuse":"true", "blurid":"${userBlurId}"}', 'NONE')[0].uuid[0]/>
</#function>
<#function getRandomPassword>
    <#return Fn.query(resources, 'randomPasswordGenerator', '{"count":1,"length":8,"reuse":"true", "symbols":"letter_maj,letter_min,special,digit","processId":"PROCESS_TESTU", "blurid":"${userBlurId}"}', 'NONE')[0].password[0]/>
</#function>
<#-- ****************************************************************************** -->
<#-- ****************************** MACRO DE LOGGING ****************************** -->
<#-- ****************************************************************************** -->
<#macro log level message>
	<#assign void = Fn.log(level, message)>
</#macro>
<#macro ANONYMIZE user>
    <#list user["./*"] as attribute>
        <#switch attribute?node_name>
            <#case "Id">
        <id>${randomId}</id>
                <#break>
            <#case "Nom">
        <Nom>${randomUserLastName}</Nom>
                <#break>
            <#case "Prenom">
        <Prenom>${randomUserFirstName}</Prenom>
                <#break>
            <#case "DateDeNaissance">
        <DateDeNaissance>${randomDOB}</DateDeNaissance>
                <#break>
            <#case "AdresseMail">
        <AdresseMail>${randomUserMail}</AdresseMail>
                <#break>
            <#case "Adresse">
        <Adresse>${randomAddress}</Adresse>
                <#break>
            <#case "CodePostale">
        <CodePostale>${randomPostcode}</CodePostale>
                <#break>
            <#case "Ville">
        <Ville>${randomCity}</Ville>
                <#break>
            <#case "UAI">
        <UAI>${randomUAI}</UAI>
                <#break>
            <#case "UID">
        <UID>${randomUid}</UID>
                <#break>
            <#case "UUID">
        <UUID>${randomUUid}</UUID>
                <#break>
            <#case "Password">
        <Password>${randomPassword}</Password>
                <#break>
            <#default>
        <${attribute?node_name}>${attribute}</${attribute?node_name}>
        </#switch>
    </#list>
</#macro>
<#-- ****************************************************************************** -->
<#-- ***************************** DEBUT DU SCRIPT ******************************** -->
<#-- ****************************************************************************** -->
<#setting time_zone="Europe/Paris">
<#setting datetime_format="iso_nz">
<#assign fichiersAAnonymiser=Fn.getEntries(resources,'listFichiersAAnonymiser','')/>
<#if fichiersAAnonymiser?has_content && fichiersAAnonymiser?size == 1>
<users>
    <#assign users=Fn.getNodeModel(fichiersAAnonymiser[0].path[0])["/users/user"]/>
    <#assign userBlurId=0/>
    <#if users[0]["Id"]?string?number == 309582> <#-- To allow multiple anonymization for test3 -->
        <#assign userBlurId=.now?long?int/>
    </#if>
    <#list users as user>
    <user>
        <#assign randomId=getRandomInteger()/>
	    <#assign randomUser=getRandomUser(user)[0]/>
        <#assign randomUserFirstName=randomUser.name_first[0]/>
        <#assign randomUserLastName=randomUser.name_last[0]/>
        <#assign randomDOB=getRandomDOB(user)/>
        <#assign randomUserMail=getRandomMail()/>
        <#assign randomAddress=randomUser.location_street_name[0]/>
        <#assign randomPostcode=randomUser.location_postcode[0]/>
        <#assign randomCity=randomUser.location_city[0]?cap_first/>
        <#assign randomUAI=getRandomUAI()/>
        <#assign randomUid=getRandomUid()/>
        <#assign randomUUid=getRandomUUid()/>
        <#assign randomPassword=getRandomPassword()?xml/>
        <@ANONYMIZE user=user/>
    </user>
	</#list>
</users>
</#if>