# Addon de l'ETL Alambic - cookbook : atelier de développement

![Cook book](img/header.png "Cook book")

## Description du addon

Ce addon est destiné à collecter des exemples d'utilisation du produit Alambic qui
peuvent être repris pour le développement de addon métiers.

## Recettes

. Carpaccio de WEB services
. Galette complète supplément LDAP
. Salade d'API et de requêtes XPATH

## Explication

Le RGPD est entré en vigueur le 25 mai 2018. Il exige que les organismes mettent en
œuvre les « mesures techniques et organisationnelles appropriées » pour être en mesure
de démontrer leur conformité. Il est donc plus difficile d’avoir des données
représentatives de nos usagers sur les plateformes de test, de qualification et de
développement de nos infrastructures, sans les mesures de sécurité nécessaires.
Cependant, les activités de test et de développement doivent pouvoir se faire.

Pour répondre à cet enjeu, l’académie de Rennes a mis en œuvre une solution de
production de jeux de données représentatifs de la communauté éducative et de ses
structures. Ces jeux de données sont utilisés afin de qualifier l’offre de services portée
par le ministère de l’Éducation nationale, l’académie de Rennes et les collectivités
territoriales : EduConnect, ENT territorial et le GAR.

L’ensemble des régions académiques agrègent les données issues de leurs
établissements scolaires (premier et second degré, public/privé) et des SI RH
(public/privé), au sein d’un « annuaire académique fédérateur » (AAF).

Selon le périmètre, une grammaire d’export ou une autre sera adoptée.

Afin de disposer de jeux de données réalistes et représentatifs pour les phases de
qualification d’un projet, tout en restant conforme au RGPD, l’académie de Rennes a
fait le choix d’utiliser les exports AAF après les avoir au préalable anonymisés.

## Objectif du projet
Nous verrons ensemble au travers de nombreuse illustration, comment ce mécanisme d'anonymisation
est mis en place suivant les valeurs à anonymiser au travers de l'utilisation des générateurs.

## Générateurs utilisés

* randomIntegerGenerator
* randomUserGenerator
* randomDateGenerator
* randomMailGenerator
* randomUAIGenerator
* randomUidGenerator
* randomUUidGenerator
* randomPasswordGenerator
    +++ lien vers doc alambic



## Explication du fonctionnement des tests
Pour exemple, dans le cas ci-dessous, le generateur d'integer permet l'anonymisation d'un integer, s'appuyant sur une base de donnée chargée côté postgres permettant de choisir un integer aleatoire, qui sera lier par le contexte et le blurid à l'integer source.

Exemple:
Pour un id donnée, on va faire appel au générateur au travers d'une requête :

`Fn.query(resources, 'randomIntegerGenerator', '{"count":1,"minValue":100000, "maxValue":999999, "processId":"PROCESS_TESTU","reuse":"true", "blurid":"${userBlurId}"}', 'NONE')[0].value[0]`


## Comment est-ce que ça fonctionne sur un environnement de production ?
