# Relatório 1 - *Software Processes*

## Descrição do projeto

O projeto [**“ownCloud”**](https://owncloud.org/), desenvolvido de uma forma transparente pela comunidade *open source*, corresponde a um *software* de cliente-servidor para guardar ficheiros na nuvem.

Estão desenvolvidas aplicações tanto para *desktop* como para *mobile*, sendo que para *desktop* existem versões para Windows, Mac e Linux, e para *mobile* versões para Android, BlackBerry e iOS.

O projeto do nosso grupo focar-se-á no desenvolvimento do projeto [**Android**](https://play.google.com/store/apps/details?id=com.owncloud.android).

![owncloud](/ESOF-docs/resources/ownCloud2.png)

A aplicação permite:
* Guardar ficheiros, calendários, contatos e emails;
* Partilhar os ficheiros com outros utilizadores ou grupos de utilizadores;
* Definir datas de expiração da partilha;
* Proteger a partilha com palavra-chave;
* Permitir ou não editar.

Posto isto, tanto utilizadores particulares que usem um [**servidor gratuito**](https://owncloud.org/providers/), assim como grandes empresas que utilizem uma [**subscrição empresarial**](https://owncloud.com/), podem ter os seus ficheiros sincronizados de uma forma segura e descomplicada em todos os seus dispositivos, uma vez que são estes que controlam os servidores.

A aplicação permite usar várias contas e trocar entre elas, mas só uma conta pode estar a uso de cada vez. Fornece algumas vantagens relativamente à interface Web, uma vez que permite uma sincronização automática dos ficheiros, assim como adicionar ficheiros diretamente do armazenamento do dispositivo.

Duas aplicações familiares a todos e que são bastante parecidas com esta são o *Google Drive* e o *OneDrive*.

## Processos de desenvolvimento

### Descrição do processo de desenvolvimento

O processo de desenvolvimento adotado pelos programadores foi o [*Open Planning Process*](https://owncloud.org/blog/open-planning-process/), introduzido no ownCloud por Matt Richards. Neste tipo de processo, cada pessoa pensa numa funcionalidade que gostaria de ver implementada e, se souber, implementa, senão pede para implementar. Isto permite e encoraja a colaboração, participação e apreciação do *feedback* dos utilizadores. Desta forma, o projeto tem uma lista de objetivos planeados, recolhidos das ideias e sugestões dadas, sendo estes prioritizados (através do número de *likes* que recebem) e divididos em pacotes para serem tratados pelos contribuidores. Com isto, conectam-se as pessoas, fazendo com que este processo leve a um plano de desenvolvimento partilhado, visível, transparente e democrático.
Para sugerir uma nova funcionalidade, o interessado deve:
* Criar um tópico em ownCloud Central;
* Descrever a funcionalidade que pretende que seja implementada;
* Colecionar *likes* na sua proposta;
* Discutir a implementação/design com a comunidade e organizar o desenvolvimento;
* Esperar que a sua feature seja incluída numa próxima *release*.

De acordo com os diferentes ficheiros de código presentes no projecto, os contribuidores usam, maioritariamente, *Incremental Development and Delivery*.

#### Atividade

O projeto encontra-se ativo com uma média de 20 commits por semana, no último ano.
No momento em que redigimos o relatório, existiam 306 *issues*. Em relação aos *pull requests*, existiam 46 pedidos em aberto, o que demonstra que este projeto se encontra bastante ativo, com cerca de 50 contribuidores.

#### Objetivos

Existem 2 versões planeadas para serem lançadas. A primeira versão encontra-se 23% completa, sendo atualizada com bastante regularidade. No que diz respeito à segunda versão, ainda não se encontra a ser implementada.
As principais funcionalidades que pretendem implementar são:
* Autenticação baseada em *token*
* Favoritos
* Melhor sincronização

### Opiniões, Críticas e Alternativas

Na opinião do grupo, o processo de desenvolvimento usado é uma boa opção no sentido em que qualquer pessoa consegue contribuir com facilidade. Por outro lado, essa facilidade de contribuição leva a que cada contribuidor possa usar um processo de desenvolvimento pessoal, tornando-se mais difícil para um novo contribuidor entender a estrutura do projeto e, dessa forma, contribuir para ele sem interferir com o código de outros contribuidores.

Outros processos de desenvolvimento também aplicáveis seriam *Software prototyping*, *Incremental development and delivery* e *Test-Driven Development*.

Ao utilizar *Software prototyping*, os desenvolvedores conseguiriam criar versões incompletas da aplicação que posteriormente seriam completadas. Desta forma, conseguiriam obter *feedback* dos utilizadores aos quais fariam chegar essa versão, fazendo desde cedo alterações que poderiam evitar erros que se propagariam com o desenrolar do projeto, assim como ajustes que levariam a uma melhor aceitação por parte da comunidade.

O processo *Incremental development and delivery* seria uma boa alternativa, pois permite desenvolver funcionalidades, lançá-las e analisar a resposta do público/cliente, de forma a melhorar a funcionalidade ou eliminá-la sem ter sido necessário nem um grande esforço nem muitos recursos para desenvolvê-la. Este processo reduz o risco de se desenvolver um grande projecto que não serve as necessidades do público-alvo. Por outro lado, com cada incremento, torna-se mais difícil estruturar correctamente o código, podendo ser necessário criar um projecto novo quando se chegar a um ponto de rotura.

O processo *Test-Driven Development* permitiria que o projeto tivesse uma lista de objetivos planeados e que cada novo contribuidor criasse a lista de testes a serem cumpridos. Depois o contribuidor desenvolveria o código de maneira a passar nesses novos testes.


## Contribuições

Diogo Cruz - up201105483@fe.up.pt - 25%

Luís Barbosa - up201405729@fe.up.pt - 25%

Paulo Santos - up201403745@fe.up.pt - 25%

Sérgio Ferreira - up201403074@fe.up.pt - 25%

## Bibliografia

* android/user_manual/android_app.rst (diretório no repositório do GitHub)
* [ownCloud Forum](https://central.owncloud.org/t/what-is-owncloud-development-process/3239)
* [GitHub Issue](https://github.com/owncloud/android/issues/1822)
* [Patrick Maier: Open Planning Process @ ownClouders' YouTube account](https://www.youtube.com/watch?v=276KkF0AzVU)
* [David A. Velasco: Meanwhile, on the Android side... @ ownClouders' YouTube account](https://www.youtube.com/watch?v=NTVVGphd4As) 
