# Relatório 2 - *Requirements Elicitation*

## Requesitos

Como foi referido no relatório anterior, o processo de desenvolvimento, uma vez que é *Open Planning Process*, é bastante simples e
informal no que diz respeito a requisitos, pois *SCRUM* e outros métodos *agile* não dão muita importância a esse tipo de formalismo.

Um pedido de implementação de uma funcionalidade é caracterizado como uma *user storie*. Estes são, inicialmente, uma breve descrição
acerca do que o utilizador pretende que seja implementado, com uma sintaxe idêntica a: 
"Sendo um [tipo_de_utilizador], quero [fazer_algo] para [obter_algo]." 
Por exemplo, "Sendo um utilizador da aplicação móvel eu quero mover os meus ficheiros para outra pasta de forma a conseguir manter a minha
conta ordenada."

Partindo desta descrição inicial, em qualquer momento antes da *user storie* ser passada para uma *sprint*, o responsável pela decisão
de prioridades (*product owner*) pode acrescentar critérios de aceitação. Desta forma, serão pedidas informações relevantes
para a implementação, como por exemplo detalhes acerca da interface e restrições que se considerem importantes. 

Durante o planeamento da *sprint*, a equipa responsável pelo desenvolvimento da funcionalidade tem uma reunião com o responsável para obter todas as informações
e esclarecer possíveis dúvidas, assegurando que todos estejam no mesmo caminho. Pode ser necessário acrescentar mais critérios, ou alterar os existentes.
No final desta reunião, a equipa estima o esforço necessário para implementar a *user storie* e, se necessário, subdivide-a em outras mais pequenas que possam ir sendo implementadas ao longo das *sprints* seguintes.

Uma vez que a *sprint* começou, os critérios não devem ser alterados. É normal que surjam questões e pode-se consultar o responsável 
para as esclarecer, assim como deixar a equipa tomar decisões, restrigindo-se aos critérios já existentes. 
Na revisão da *sprint*, a demonstração é uma forma de validar os critérios de aceitação com o responsável pela *user storie*, embora, por vezes, as funcionalidades sejam demasiado complexas para serem demonstradas. 

Por fim, é da responsabilidade da equipa (especialmente dos engenheiros Q&A) garantir que os critérios de aceitação estão reunidos e
demonstrar apenas *user stories* que estejam realmente concluídas.

## Requesitos específicos e funcionalidades (Requesitos funcionais e não funcionais)
Ao iniciar a aplicação é pedido o endereço de um servidor da *OwnCloud*, um nome de utilizador e palavra-chave. Após iniciar-se sessão, são apresentados os ficheiros e pastas guardados no sistema. Portanto, é necessário um telemóvel com sistema operativo *Android* ou *iOS* para se poder instalar a aplicação da *OwnCloud* e uma conta no sistema para iniciar sessão.
A aplicação permite ver o nome do utilizador, seleccionar a vista que mostra todos os ficheiros, seleccionar a vista que mostra os ficheiros carregados e aceder às definições. Na vista "Todos os ficheiros" é possível actualizar a conta, ordenar os ficheiros de diferentes formas e mudar a forma como são vistos os ficheiros (grelha ou lista). Na vista "Carregamentos" existe uma opção chamada "A tentativa falhou", para seleccionar os carregamentos cuja tentativa falhou. Para além dessa opção é possível limpar os carregamentos que falharam, que foram carregados com sucesso ou os concluídos. Por fim, nas definições, é possível adicionar mais contas e transitar entre elas, definir se os carregamentos de imagens e/ou vídeos podem ser instantâneos e proteger a aplicação por código. Na mesma nas definições, há mais algumas opções pouco importantes que são: "Ajuda" (hiperligação para o sítio online da aplicação), "Recomendar a um amigo" (por e-mail), "Opinião" (para enviar um e-mail aos desenvolvedores), "Registo de Alterações" (corresponde ao histórico de alterações) e a versão da aplicação.

## Casos de uso

![UseCases](/ESOF-docs/resources/usecases.png)

## Modelo de domínio

## Contribuições
