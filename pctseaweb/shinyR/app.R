#
# This is a Shiny web application. You can run the application by clicking
# the 'Run App' button above.
#
# Find out more about building applications with Shiny here:
#
#    http://shiny.rstudio.com/
#

library(shiny)
library(dplyr)
library(stringr)
library(ggplot2)
library(sjmisc)
library(tidyverse)
library(data.table)
library(promises)
library(future)
library(tools)
library(filesstrings)
# plan(multisession)
plan(multicore)

options(shiny.maxRequestSize = 120*1024^2)

createPlotWithCorrelations <- function(table, correlation_threshold, cell_type){
    req(table)
    # create a new column that says whether the correlation pass the threshold or not
    table[,"positive"] <- table$Pearson.s.correlation > correlation_threshold
    # remove correlations that are NaN
    table <- table[!is.na(table$Pearson.s.correlation),]
    # create a rank column
    table[, "rank"] <- c(1:length(table[,1]))
    # title
    my_title <- "Ranks of cells by Pearson's correlation"
    # filter by cell type
    if(!missing(cell_type)){
        if(!sjmisc::is_empty(cell_type)){
            table <- table[table$Cell.type == cell_type,]
            my_title <- paste("Ranks of cells of type '",cell_type,  "' by Pearson's correlation", sep="")
        }else{
            return()
        }
    }
    ggplot(data = table,
           aes(
               x = rank,
               y = Pearson.s.correlation,
               group = positive,
               fill = positive)) + # color by Comprador
        labs(title = my_title, x = "cell #", y = "Pearson's correlation") +
        geom_line(aes(color=positive))+
        theme_classic()
}

createPlotWithScoreCalculation <- function(table, cell_type){
    req(cell_type)
    table <- table[table$cell_type == cell_type, ]
    type <- table[table$type_or_other == 'TYPE', ]
    type <- type %>% select( 3:ncol(.) )
    type <- t(type)
    other <- table[table$type_or_other == 'OTHER', ]
    other <- other %>% select( 3:ncol(.) )
    other <- t(other)
    new_table <- data.frame(type, other)
    names(new_table)[1] <- cell_type
    names(new_table)[2] <- "others"
    new_table[, "rank"] <- c(1:length(new_table[,1]))
    new_table <- melt(data = new_table, id.vars = "rank", variable.name = "cell_type")
    ggplot(data = new_table,
           aes(
               x = rank,
               y = value,
               group = cell_type)) +
        labs(title = paste("Enrichment score calculation for cell type: '",cell_type, "'", sep=""), x = "cell #", y = "Cumulative Probability") +
        geom_line(aes(color = cell_type))+
        theme_classic() +
        xlim(1,max(new_table$rank)) +
        ylim(0,1)
}

# Define UI for application that draws a histogram
ui <- fluidPage(title = "PCTSEA",
                br(),
                sidebarLayout(
                    sidebarPanel(width = 2,
                                 conditionalPanel(
                                     condition = "input.tabs == 'Import data'",
                                     uiOutput(outputId = "importSideControlUI")

                                 ),
                                 conditionalPanel(
                                     condition = "input.tabs == 'Table'",
                                     p("Here you have the main output table")
                                 ),
                                 conditionalPanel(
                                     condition = "input.tabs == 'Correlations'",
                                     p("Select cell type:"),
                                     selectInput(inputId = "selectCellType", label = "Cell type", choices = c())
                                 )
                    ),
                    mainPanel(
                        tabsetPanel(id = "tabs",
                                    tabPanel("Import data",

                                             br(),
                                             uiOutput(outputId = "importControlUI"),


                                    ),
                                    tabPanel("Table",
                                             dataTableOutput(outputId = "enrichmentDataTable")
                                    ),
                                    tabPanel("Correlations",
                                             fluidRow(
                                                 column(6, plotOutput(outputId = "correlationsPlot")),
                                                 column(6, plotOutput(outputId = "umapPlot"))
                                             ),
                                             fluidRow(
                                                 column(6, plotOutput(outputId = "cellTypeCorrelationsPlot")),
                                                 column(6, plotOutput(outputId = "cellTypeScoreCalculationPlot"))
                                             ),
                                             fluidRow(
                                                 dataTableOutput(outputId = "correlationsDataTable")
                                             )
                                    )
                        )
                    )
                )
)

# Define server logic required to draw a histogram
server <- function(input, output, session) {
    observe({
        query <-parseQueryString(session$clientData$url_search)
        if (!is.null(query$file)) {
            inputFileName <- query$file

            # side panel
            output$importSideControlUI <- renderUI({
                tagList(
                    h4(paste("Data from '", inputFileName, "' results")),
                )
            })



            # get file from local path and unzip them
            output$inputDataStatus <- renderText(paste("Importing data from file '", inputFileName, "'...", sep = ""))
            zipfilepath = paste('data/', inputFileName, sep = '')
            if (!file.exists(zipfilepath)){
                output$inputFilesPathError <- renderText("File not found")
                return()
            }
            req(zipfilepath)
            # folderTo = tempdir()
            browser()
            folderTo <- paste(dirname(zipfilepath), "/", tools::file_path_sans_ext(basename(zipfilepath)), sep = "")
            withProgress({unzip(zipfilepath, exdir=folderTo)
                setProgress(message = "Results unzipped")
            },
            message = "Unzipping results...",
            detail = "This just will take a few seconds")
            rv$unziped_files <- folderTo


        }else{
            output$importSideControlUI <- renderUI({
                tagList(
                    ##############################################
                    p("PcTSEA generates a zip file with all the output files compiled together."),
                    p("Here you can upload that zip file and it will be imported to show the results"),
                    p("After uploading the file, click on 'Import'")
                    ##############################################
                )
            })
            output$importControlUI <- renderUI({
                #########################################
                 tagList(
                    fluidRow(
                        column(width = 8,
                               p("Use one of the options and click on 'Import' button below:")
                        )
                    ),
                    fluidRow(
                        column(width = 8,
                               textOutput(outputId = "inputDataError")
                        )
                    ),
                    wellPanel(
                        fluidRow(
                            column(width = 8,
                                   fileInput(inputId = "inputUploadedFile", label = "Upload your PCTSEA results (zip)", multiple = FALSE)
                            )
                        )
                    ),
                    fluidRow(
                        column(width = 1, actionButton(inputId = "importButton", label = "Import")),
                        column(width = 7, textOutput(outputId = "inputDataStatus"))
                    )
                 )

                #####################################
            })
        }
    })




    rv <- reactiveValues(errorMessage="",
                         enrichmentTable=NULL,
                         correlationsTable=NULL,
                         scoresCalculationsTable=NULL,
                         unziped_files=NULL
    )
    output$data_loaded <- reactive({FALSE})
    outputOptions(output, "data_loaded", suspendWhenHidden = FALSE)

    # get files by uploading them and unzip them
    observeEvent(input$inputUploadedFile, {
        zipfilepath = input$inputUploadedFile[1,"datapath"]
        # copy file to data
        file.move()
        # folderTo = tempdir()
        folderTo <- paste(dirname(zipfilepath), "/", tools::file_path_sans_ext(basename(zipfilepath)), sep = "")
        browser()
        setProgress({unzip(zipfilepath, exdir=folderTo)},
                    message = "Unzipping results...", detail = "This just will take a few seconds")
        rv$unziped_files <- folderTo
    })


    # select the enrichment file
    enrichment_file <- eventReactive(rv$unziped_files,{
        folder = rv$unziped_files
        paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*cell_types_enrichment.txt")[1], sep = "")
    })
    # read enrichment file into a table in background
    enrichment_table <- eventReactive(enrichment_file(),{
        output$inputDataStatus <- renderText("asdfasdfta...")
        withProgress({
            t <- fread(file = enrichment_file(), header = TRUE, skip = 33, sep = "\t")
            setProgress(value = 0.5)
            t = t[ews>0,] # take only with positives ews
            setProgress(value = 1)
            return(t)
        }, message = "Reading enrichment table", detail = "Please wait for a few seconds...")
    })
    # plot the table as soon as is loaded
    output$enrichmentDataTable <- renderDataTable({
        enrichment_table()
    },
    options = list(pageLength = 20)
    )

    # select the correlations file
    correlations_file <- eventReactive(rv$unziped_files,{
        folder = rv$unziped_files
        paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*correlations.txt")[1], sep = "")
    })
    # read the file
    correlations_table <- eventReactive(correlations_file(),{
        withProgress({
            table = fread(file = correlations_file(), header = TRUE, sep = "\t", showProgress = TRUE)
            setProgress(value = 1)
            table
        }, message = "Reading correlations file", detail = "This can take a few seconds. Please wait...")
    })
    # plot the table as soon as is loaded
    output$correlationsDataTable <- renderDataTable({
        t <-correlations_table()
        browser()

        t <- t[,"Pearson's correlation">0.3]

    }, options = list(pageLength = 20)
    )
    # plot the correlation plot
    output$correlationsPlot <- renderPlot(correlations_table() %>% createPlotWithCorrelations(., 0.1))

    # select the scores file
    scores_file <- eventReactive(rv$unziped_files,{
        folder = rv$unziped_files
        paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*score_calculations.txt")[1], sep = "")
    })
    # read the file
    # scores_table <- eventReactive(scores_file(),{
    #     withProgress({
    #          table = read.csv(file = scores_file(), header = TRUE, sep = "\t")
    #     })
    # })
    output$inputDataError <- renderText(rv$errorMessage)




    cellTypes = reactive({
        table <- rv$enrichmentTable
        req(table)
        table = table[table$ews>0,]
        table <- table[!is.na(table$Cell.type),]
        unique_cell_types = unique(rv$enrichmentTable$Cell.type)
        unique_cell_types <- sort(unique_cell_types)
        unique_cell_types
    })
    observe({
        updateSelectInput(session, "selectCellType",
                          choices = cellTypes())
    })
    output$cellTypeCorrelationsPlot <- renderPlot(createPlotWithCorrelations(isolate({rv$correlationsTable}), 0.1, input$selectCellType))

    output$cellTypeScoreCalculationPlot <- renderPlot(createPlotWithScoreCalculation(isolate({rv$scoresCalculationsTable}), input$selectCellType))
}

# Run the application
shinyApp(ui = ui, server = server)
