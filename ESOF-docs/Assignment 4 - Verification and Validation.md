# Relatório 4 - *Verification and Validation*

O objetivo deste relatório é documentar o estado atual do projeto no que respeita à verificação e validação. O uso de testes é uma mais valia para garantir a qualidade do projeto, pois permitem determinar a existência de erros importantes. No entanto, os testes não provam que o código não tenha falhas.
Decidimos optar pelas ferramentas de teste (static/dynamic).

## *Software Testability and Reviews*

### Controlabilidade
A controlabilidade, por definição, é o grau que permite controlar o estado do componente a ser testado (CUT - *Component Under Test*).

Analisando os testes do *OwnCloud*, verificamos que são bastantes específicos, ou seja, a sua controlabilidade é tanto maior quanto a especifidade do teste.

### Observabilidade
Este ponto refere-se ao grau no qual é possível observar os resultados intermediários e finais dos testes.

Para obter o resultado dos testes, é necessário corre-los no *Eclipse* com *JUnit* ou com a ferramenta *Appiun*. No entanto, os testes que estão atualmente no *branch master* estão obsoletos, estando a serem desenvolvidos novos testes *Espresso* noutro *branch*.

### Isolabilidade
A isolabilidade representa o grau em que cada componente pode ser testado isoladamente.

Assim, a isolabilidade é tanto maior quanto menos se relacionem os módulos uns com os outros. No caso do *OwnCloud*, a maior parte dos módulos estão relacionados entre si, o que dificulta o teste de cada módulo isoladamente.

### Separação de Responsabilidades
A separação de responsabilidades define se o componente a ser testado tem uma responsabilidade bem definida.

Para que a estrutura do projeto fique bem organizada e de fácil compreensão e acesso, cada módulo deve estar bem definido, evitando assim que o código fique misturado e menos eficiente. No caso do *OwnCloud*, a sua estrutura está bem definida. Os principais desenvolvedores optaram por criar vários *packages* de forma a que cada funcionalidade fique bem definida, sendo os seus sub-problemas resolvidos no seu interior.

### Perceptibilidade
A perceptibilidade avalia o grau em que o componente em teste está autoexplicativo e documentado.

Avaliando os testes disponíveis no projeto, determinamos que o nome dos mesmo é claro e, por isso, autoexplicativo. Isto permite ao utilizador verificar com muita facilidade qual o teste que falhou e a localização do erro.

### Heterogeneidade
Determina o grau que o uso de diversas tecnologias requer, para usar diversos métodos de ensaio e ferramentas em paralelo.

Como o *OwnCloud* é um projeto com ainda alguns contribuidores, é utilizado um canal IRC para as pessoas que pretendam contribuir possam comunicar entre si e com os principais desenvolvedores. Para facilitar a execução dos testes é também utilizada a biblioteca *JUnit*.

## *Report Test Statistics and analytics*
Number of test cases, percentage of coverage, number of flaky tests, etc. (see links of projects in moodle for inspiration)

## *Identify a new bug and/or correct a bug*
(identification: 4 points; correction: 2 points)
You think your project has no bugs?! Then, you do need to have a compelling story for us to credit you 6pts! 
