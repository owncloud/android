# Relatório 5: Manutenção/Evolução do *Software*

O objetivo deste relatório é documentar o processo seguido para desenvolver a funcionalidade que adicionamos ao projeto e abordar a análise do impacto das alterações realizadas.

## Manutenção do *software* (métricas SIG)

[![BCH compliance](https://bettercodehub.com/edge/badge/PauloSantos13/android)](https://bettercodehub.com)

Utilizamos a ferramenta [**Better Code Hub**](https://bettercodehub.com) para avaliar a qualidade do *ownCloud*. A avaliação é feita com os seguintes tópicos:
<ul>
    <li>[ ] Escrever pequenos pedaços de código</li>
    <li>[ ] Escrever pedaços de código simples</li>
    <li>[ ] Não repetir código</li>
    <li>[ ] Manter as unidades de interface pequenas</li>
    <li>[ ] Separar as funcionalidades por módulos</li>
    <li>[x] Arquitetura com componentes independentes</li>
    <li>[ ] Manter as componentes arquiteturais equilibradas</li>
    <li>[x] Manter a quantidade de código pequena</li>
    <li>[ ] Automação de testes</li>
    <li>[x] Manter o código limpo</li>
</ul>
Analisando estas características, o nosso projeto obteve uma classificação de 3 em 10.

**Escrever pequenos pedaços de código** é o mesmo que dizer encapsular o código. No caso deste projeto nota-se que não preocupação em criar funções para por exemplo atribuir valores predefinidos a variáveis, ficando uma função longa com um *for* muito extenso.
![for muito extenso](/ESOF-docs/resources/FileDataStorageManager.updateSharedFiles(Collection).png)

**Escrever pedaços de código simples** é ter pouca densidade de *branch points* (if, for, while, etc.), métodos que dividem o caminho do código e tornam mais complicada a análise por outro programador.
![for muito extenso](/ESOF-docs/resources/LocalFileListAdapter.getView(int,View,ViewGroup).png)

**Não repetir código** é uma boa prática porque se for encontrado um *bug* terá que se o corrigir em todas as situações em que se copiou o código. Existem no projeto várias situações em que blocos de texto foram copiados para outros ficheiros, o que é uma má prática.

**Manter as unidades de interface pequenas** revela um bom encapsulamento e o contrário demonstra a necessidade de repensar a organização das estruturas e a necessidade de estruturas intermédias mais pequenas. Isto pode ser analisado pelo número de parâmetros que as funções precisam. Foram encontrado 7 casos em que a função tinha mais de 6 parâmetros. 
![demasiados parâmetros](/ESOF-docs/resources/CompatScroller.fling(int,int,int,int,int,int,int,int).png)

**Separar as funcionalidades por módulos** faz com que ao mudar um módulo não se afete os restantes e como tal seja necessária uma menor reestruturação das funções. Existem 11 módulos com mais de 50 chamadas, ou seja, uma alteração num desses módulos implica ir confirmar se todas essas chamadas continuam a funcionar.
![demasiados parâmetros](/ESOF-docs/resources/Separar funcionalidades em módulos.png)

A **arquitetura com componentes independentes** permite manter um subprojeto caso seja alterado para um novo ambiente sem os restantes subprojetos. Neste caso os principais subprojetos são o *com\owncloud\android* e o *third_parties*. Sendo que existe 5 ou menos chamadas entre subprojetos, é considerado que o projeto tem os componentes independentes entre si.

**Manter as componentes arquiteturais equilibradas** faz com que seja mais fácil localizar o código. Como quase a totalidade do código se encontra em *com\owncloud\android*, é considerado um projeto desequilibrado.

**Manter a quantidade de código pequena** torna possível compreender todo o projeto e ser fácil saber todas as funções do projeto. O projeto foi avaliado em 20 *man-year*, o que é um valor no limear do aceitável.

A **Automação de testes** dá uma maior segurançao para gerar novo código pois permite saber se o novo código é compatível com o antigo por exemplo. Como já foi referido [no relatório anterior](/ESOF-docs/Assignment 4 - Verification and Validation.md), o projeto *ownCloud* ainda não tem uma implementação de testes abrangente e essa análise foi feita nesse relatório.

**Manter o código limpo** é uma boa prática. Ao desenvolver o código é normal deixar notas pessoais para quando mais tarde se voltar a trabalhar ser fácil retomar o raciocínio, mas depois de entregue é necessário remover esses apontamentos. O projeto tem alguns *code smells* que se nota que são partes a ser completadas que o programador não acabou, caso dos *else* vazios nos condicionais ou linhas comentadas, mas ainda assim numa quantidade aceitável.

A opinião do grupo face ao baixo resultado de aprovação dos testes é que, ao ser um projeto livre que qualquer um pode contribuir, os programadores não têm cuidado em verificar o código dos colegas, não fazendo refactoring.

![compliance](/ESOF-docs/resources/compliance.png)

Discuss Software Maintainability using the SIG metrics (plus add the badge to this .md file).

## Processo de evolução (análise do impacto das alterações e implementação)

Após uma análise detalhada das funcionalidades que a aplicaçao do *ownCloud* permite ao utilizador, o grupo achou que estava em falta a informação sobre o espaço que o utilizador já ocupou na *cloud* relativamente ao espaço total, dado o propósito da aplicação.

Desta forma, falamos com o responsável pela aplicação, e este deu-nos algumas dicas por onde devíamos começar, concordando que era uma *feature* interessante para ser implementada.

Recorrendo à funcionalidade *Android Monitor* do *Android Studio*, o grupo tentou entender que módulos eram chamados enquanto a aplicação corria, descobrindo que partes de código seriam necessárias alterar.

Uma vez que ao iniciar a aplicação é logo chamada a operação **RefreshFolderOperation** do *package* *com.owncloud.android.operations*, pensamos ser o melhor sítio para proceder à chamada de uma nova função **updateUserQuota**, atualizando as informações acerca do espaço ocupado sempre que o utilizador faz *refresh* da página.

![callUpdateUserQuota](/ESOF-docs/resources/callUpdateUserQuota.PNG)

A função **updateUserQuota** consiste na criação e execução de uma instância da classe **GetRemoteUserQuotaOperation** (contida no *package com.owncloud.android.lib.resources.users*). Esta execução permite aceder às informações da quota de espaço de um utilizador, no servidor onde este possui a sua conta, através dos métodos *getFree()*, *getUsed()* e *getTotal()*. 

![updateUserQuota](/ESOF-docs/resources/updateUserQuota.PNG)

Como estes dados têm que ser guardados numa forma persistente para conseguirmos aceder a estes noutros módulos, recorremos à base de dados local (**mStoreManager**, instância da classe **FileDataStorageManager** do *package com.owncloud.android.datamodel*) onde criamos uma função **setQuota** para definirmos os dados.

![fileDataStoreManager](/ESOF-docs/resources/fileDataStoreManager.PNG)

Achamos que o melhor local para mostrar ao utilizador esta informação seria no *footer* da página inicial, uma vez que neste já era mostrado o número de ficheiros e pastas que existiam na *cloud*, completando assim essa informação. Verificamos que este era atualizado na função **updateLayout** da classe **OCFileListFragment** do *package com.owncloud.android.ui.fragment*. Esta função é chamada por outra denominada **listDirectory**, e é nesta que vamos buscar os dados que armazenamos anteriormente na base de dados acerca da quota do utilizador.

![listDirectory](/ESOF-docs/resources/listDirectory.PNG)

A função **updateLayout** recebe agora os parâmetros *usedQuota* e *totalQuota*, definindo o *footer* com os novos dados.

![updateLayout](/ESOF-docs/resources/updateLayout.PNG)

Esse *footer* é criado na função **generateFooterText**, onde inicialmente era mostrado apenas o número de ficheiros e pastas existentes no diretório raiz, contendo agora as informações sobre o espaço utilizado e o espaço total do servidor no qual o utilizador possui uma conta.

![generateFooterText](/ESOF-docs/resources/generateFooterText.PNG)

Faz-se uso da função **formatShortFileSize** para formatar o tamanho do espaço utilizado e total para o mais adequado, uma vez que os parâmetros *usedQuota* e *totalQuota* são expressos em *bytes*.

Mostra-se, a seguir, o resultado antes e depois das alterações: 

<img src="/ESOF-docs/resources/beforeChanges.png" align="center" width="400" >
<img src="/ESOF-docs/resources/afterChanges.png" align="center" width="400" >

## Link para o *pull request*

## Contribuições
