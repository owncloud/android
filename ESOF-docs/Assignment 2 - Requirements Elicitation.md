# Relatório 2 - *Requirements Elicitation*

## Requisitos

Como foi referido no [**relatório anterior**](https://github.com/PauloSantos13/android/blob/master/ESOF-docs/Assignment%201%20-%20Software%20Processes.md#descrição-do-processo-de-desenvolvimento), o processo de desenvolvimento, uma vez que é *Open Planning Process*, é bastante simples e informal no que diz respeito a requisitos, pois *SCRUM* e outros métodos *agile* iriam despender muito tempo a alterar documentos que estariam em permanente mudança. Em vez de estar delineado o plano por escrito, a equipa reúne-se para organizar a próxima *sprint*, onde decide quais as novas funcionalidades que vai implementar a curto prazo. Estas funcionalidades são escolhidas a partir de uma lista de *user stories* armazenadas por ordem de prioridade na [*product backlog*](https://github.com/owncloud/android/milestone/17).

Uma *user story* é um pedido de implementação de uma funcionalidade. Estas são, inicialmente, uma breve descrição acerca do que o utilizador pretende que seja implementado, com uma sintaxe idêntica a: 
"Sendo um [tipo_de_utilizador], quero [algo_implementado] para [conseguir_realizar_algo]." 
Por exemplo, "Sendo um utilizador da aplicação móvel, quero mover os meus ficheiros para outra pasta de forma a conseguir manter a minha conta ordenada."

Partindo desta descrição inicial, em qualquer momento antes da *user story* ser passada para uma *sprint*, o responsável pela decisão de prioridades, o *product owner*, pode acrescentar critérios de aceitação. 
Desta forma, serão pedidas informações relevantes para a implementação como, por exemplo, detalhes acerca da interface e restrições que se considerem importantes.

Durante o planeamento da *sprint*, a equipa responsável pelo desenvolvimento da funcionalidade tem uma reunião com o *scrum master* para obter todas as informações e esclarecer possíveis dúvidas, assegurando assim que todos estão no mesmo caminho e sincronizados. Depois o *scrum master* reúne-se com o *product owner* para esclarecer as dúvidas da equipa. Pode ser necessário acrescentar mais critérios, ou alterar os existentes. Depois de delineado o trabalho a desenvolver nessa *sprint*, as *user stories* que serão tratadas são movidas da *backlog* para a *sprint backlog*.
No final desta reunião, a equipa estima o esforço necessário para implementar as *user stories* e, se necessário, divide-as em novas *user stories* mais pequenas que possam ser implementadas ao longo das próximas *sprints*.

Uma vez iniciada a *sprint*, os critérios não devem ser alterados. É normal que surjam questões e pode consultar-se o responsável para as esclarecer, assim como deixar a equipa tomar decisões, restrigindo-se aos critérios já existentes. 

No final da *sprint* é feita a *sprint review*. Nesta fase, a equipa mostra o trabalho ao *product owner*, que decide se as questões foram bem resolvidas ou se a *user story* não pode ser declarada como concluída, e analisa a *product backlog* para preparar a próxima *sprint*. A *product backlog* é alterada no *GitHub* tanto por desenvolvedores como por clientes e, como tal, é necessário reavaliar as prioridades e decidir quais as funcionalidades mais importantes de acordo com o *feedback* que vem da comunidade.

## Requisitos específicos e funcionalidades (Requisitos funcionais e não funcionais)

Ao iniciar a aplicação é pedido o endereço de um [**servidor suportado**](https://owncloud.org/providers/) pela *OwnCloud*, um nome de utilizador e uma palavra-chave. Após iniciar-se a sessão, são apresentados os ficheiros e pastas guardados no sistema. Portanto, é necessário um telemóvel com sistema operativo *Android*, *iOS* ou *BlackBerry* para se poder instalar a aplicação *OwnCloud* e uma conta no sistema para iniciar sessão.

A aplicação permite ver o nome do utilizador, seleccionar a vista que mostra todos os ficheiros, seleccionar a vista que mostra os ficheiros carregados e aceder às definições.

Na vista "Todos os ficheiros" é possível actualizar a conta, ordenar os ficheiros de diferentes formas e mudar a forma como são vistos os ficheiros (grelha ou lista). 

Na vista "Carregamentos" existe uma opção chamada "A tentativa falhou", para seleccionar os carregamentos cuja tentativa falhou. Para além dessa opção, é possível limpar os carregamentos que falharam, que foram carregados com sucesso ou os concluídos. 

Nas definições, é possível adicionar mais contas e transitar entre elas, definir se os carregamentos de imagens e/ou vídeos podem ser instantâneos e proteger a aplicação por código PIN, de forma a que qualquer acesso à aplicação peça esse código, mantendo-se sempre a sessão aberta. Ainda nas definições, há mais algumas opções que são: "Ajuda" (hiperligação para o sítio online da aplicação), "Recomendar a um amigo" (por e-mail), "Opinião" (para enviar um e-mail aos desenvolvedores), "Registo de Alterações" (corresponde ao histórico de alterações) e a versão da aplicação.

Para além destas opções existe um botão, no ecrã principal, que permite carregar ficheiros, carregar conteúdo de outras aplicações e criar pastas. Para cada ficheiro é possível definir como disponível *offline*, abrir com alguma aplicação externa, enviar para outra aplicação, mover, copiar, renomear, ver as propriedades e apagar. Para cada pasta é possível mover, copiar, renomear e apagar. Tanto ficheiros como pastas podem ser sincronizados e partilhados com outros utilizadores, grupos de utilizadores ou por hiperligação.

O projecto foi desenvolvido em Java.

## Casos de uso

O diagrama que se segue pretende demonstrar as sequências de transições possíveis na aplicação, do ponto de vista do utilizador. 

![UseCases](/ESOF-docs/resources/usecases.PNG)

## Modelo de domínio

Analisando o código fonte da aplicação, construiu-se o seguinte diagrama que pretende representar classes conceptuais baseadas nessa análise.

![DomainModel](/ESOF-docs/resources/domainmodel.png)

## Contribuições

Diogo Cruz - up201105483@fe.up.pt - 25%

Luís Barbosa - up201405729@fe.up.pt - 25%

Paulo Santos - up201403745@fe.up.pt - 25%

Sérgio Ferreira - up201403074@fe.up.pt - 25%

## Bibliografia

* Slides teóricos moodle
* Emails trocados com [**David Velasco**](https://github.com/davivel)
