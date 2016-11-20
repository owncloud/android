# Relatório 3 - *Software Design*

## Introdução

O modelo de vistas 4+1 de Arquitetura de *Software* permite reunir os vários pontos de vista sobre o mesmo *software* para dar uma perspectiva completa acerca deste. Este modelo baseia-se em quatro componentes, mais concretamente: vista lógica, representada pelo diagrama de pacotes do projeto; vista de implementação, representada pelo diagrama de componentes, vista de processo, representada pelos diagrama de atividades e de sequência, e a vista de *deployment*, representada pelo diagrama de *deployment*.
Quanto ao padrão arquitectural seguido pelo projecto, não recebemos resposta do nosso contacto, no entanto, consideramos que seja ou, pelo menos, aproxima-se do padrão *Client-server and N-tier systems*, pois os dados encontram-se tanto no dispositivo *Android* como no servidor, não seguindo o padrão *Repositories*, e é possível ter *n* dispositivos a sincronizar informação com o servidor.

## *Logical View*

Após uma análise à estrutura e ao código do projeto, elaboramos um diagrama, também conhecido como *UML packages diagram*, que demonstra como o sistema está estruturado, incluindo os principais *packages* e suas relações.

![LogicalView](/ESOF-docs/resources/logicalview.png)

## *Deployment View*

O diagrama de *deployment*, também conhecido como *UML deployment diagram*, permite mostrar de que forma é que os artefactos de um sistema são distribuídos em nós de *hardware*. Os artefactos de um sistema são manifestações físicas dos seus componentes de *software*, e relacionam-se com determinados componentes de *hardware*.

![DeploymentView](/ESOF-docs/resources/deploymentview.PNG)

## *Process View*
A vista de processo pode ser representada através de um diagrama de actividades ou através de um diagrama de sequência, também conhecidos como *UML activity diagram* e *UML sequence diagram*, respectivamente. Esta vista permite apresentar a forma como o sistema ou partes dos sistema funcionam. De seguida, vamos apresentar um diagrama de actividades, com um elevado nível de abstração, indicativo da forma como o sistema processa a sincronização de ficheiros. Para além desse diagrama, vamos apresentar um diagrama de sequência, com um nível de abstração consideravelmente menor, indicativo da forma como o sistema move um ficheiro, movimento o qual é feito no cliente e no servidor.

![Process View](/ESOF-docs/resources/activitydiagram.png)

![Process View](/ESOF-docs/resources/sequencediagram.png)
