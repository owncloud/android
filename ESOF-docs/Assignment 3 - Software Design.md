# Relatório 3 - *Software Design*

## Introdução

O modelo de vistas 4+1 de Arquitetura de *Software* permite reunir os vários pontos de vista sobre o mesmo *software* para dar uma perspectiva completa acerca deste. Este modelo baseia-se em quatro componentes, mais concretamente: vista lógica, representada pelo diagrama de pacotes do projeto; vista de implementação, representada pelo diagrama de componentes, vista de processo, representada pelos diagrama de atividades e de sequência, e a vista de *deployment*, representada pelo diagrama de *deployment*.
Quanto ao padrão arquitectural seguido pelo projecto, não recebemos resposta do nosso contacto. Estivemos a analisar os padrões possíveis e pensamos que sejam o *Client-server and N-tier systems* e o *Repositories*,  pois os dados encontram-se tanto nos dispositivos *Android* (ou outro SO) como no servidor e o servidor fornece ficheiros aos dispositivos, tal como os dispositivos fornecem ficheiros ao servidor. No entanto, existe uma componente *data centric*, já que o servidor é quem “manda” (fornece os dados) e é actualizado pelos clientes, quando necessário (recebendo dados novos), podendo ser mais correcto o padrão *Repositories*.

## *Logical View*

Após uma análise à estrutura e ao código do projeto, elaboramos um diagrama, também conhecido como *UML packages diagram*, que demonstra como o sistema está estruturado, incluindo os principais *packages* e suas relações.

![LogicalView](/ESOF-docs/resources/logicalview.png)

## *Development View*

O diagrama de *components*, também conhecido como *UML component diagram*, mostra as relações estabelecidas entre os componentes da aplicação, assim como entre a aplicação e os componentes externos. 
Como é uma aplicação que permite armazenar qualquer tipo de ficheiro na *cloud*, torna o diagrama mais simples, havendo apenas um caso fundamental que é a ligação *Client-server*.
O pedido para a sincronização é feito do dispositivo para o servidor, sendo a informação comunicada por meio de uma interface ao servidor que trata deste processo e atualiza os ficheiros, caso as credenciais de acesso sejam aceites.

![DevelopmentView](/ESOF-docs/resources/componentview.png)

## *Deployment View*

O diagrama de *deployment*, também conhecido como *UML deployment diagram*, permite mostrar de que forma é que os artefactos de um sistema são distribuídos em nós de *hardware*. Os artefactos de um sistema são manifestações físicas dos seus componentes de *software*, e relacionam-se com determinados componentes de *hardware*.

![DeploymentView](/ESOF-docs/resources/deploymentview.PNG)

## *Process View*
A vista de processo pode ser representada através de um diagrama de actividades ou através de um diagrama de sequência, também conhecidos como *UML activity diagram* e *UML sequence diagram*, respectivamente. Esta vista permite apresentar a forma como o sistema ou partes dos sistema funcionam. De seguida, vamos apresentar um diagrama de actividades, com um elevado nível de abstração, indicativo da forma como o sistema processa a sincronização de ficheiros. Para além desse diagrama, vamos apresentar um diagrama de sequência, com um nível de abstração consideravelmente menor, indicativo da forma como o sistema move um ficheiro, movimento o qual é feito no cliente e no servidor.

![ProcessView](/ESOF-docs/resources/activitydiagram.png)

![ProcessView](/ESOF-docs/resources/sequencediagram.png)

## Contribuições

Diogo Cruz - up201105483@fe.up.pt - 25%

Luís Barbosa - up201405729@fe.up.pt - 25%

Paulo Santos - up201403745@fe.up.pt - 25%

Sérgio Ferreira - up201403074@fe.up.pt - 25%

## Bibliografia

* Slides teóricos moodle
