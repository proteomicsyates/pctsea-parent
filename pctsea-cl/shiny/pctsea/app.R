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
                     sidebarPanel(
                         conditionalPanel(
                             condition = "output.no_data == 'TRUE'",
                             "PcTSEA generates a zip file with all the output files compiled together. There are two ways to visualize PCTSEA results: by uploading that file directly or by typing the full path to the results if pctsea was run on this server:"
                         ),

                         conditionalPanel(
                             condition = "output.no_data == 'FALSE'",
                             p("Select cell type:"),
                             selectInput(inputId = "selectCellType", label = "Cell type", choices = c())
                         )
                     ),
                     mainPanel(
                         tabsetPanel(
                             tabPanel("Import data",
                                      fluidRow(
                                          column(width = 8,
                                                 fileInput(inputId = "inputUploadedFile", label = "Upload your PCTSEA results (zip)", multiple = FALSE)
                                          )
                                      ),
                                      fluidRow(
                                          column(width = 8,
                                                 textOutput(outputId = "inputFilesError")
                                          ),
                                          br()
                                      ),

                                      fluidRow(
                                          column(width = 7,
                                                 textInput(
                                                     inputId = "inputFilePath",
                                                     label = "Type path to local (on server) file location",
                                                     width = "100%",
                                                     placeholder = "i.e. /home/your_user/pctsea_results/my_pctsea_results.zip",
                                                     value = "C:\\Users\\salvador\\Desktop\\casimir\\SARS_Cov2\\singlecell_human\\input files\\sprotein\\uniprot accs\\initial_spike_new_TYPE_results.zip"
                                                 )
                                          ),
                                          column(width = 1, actionButton(inputId = "inputFilePathButton", label = "Import"))
                                      ),
                                      fluidRow(
                                          column(width = 8,
                                                 textOutput(outputId = "inputFilesPathError")
                                          )
                                      )
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
                                          dataTableOutput(outputId = "correlationsDataTable")
                                      )
                             ),

                             tabPanel("Plots",
                                      fluidRow(
                                          column(6, plotOutput(outputId = "cellTypeCorrelationsPlot")),
                                          column(6, plotOutput(outputId = "cellTypeScoreCalculationPlot"))
                                      )
                             )
                         )
                     )
                 )
)

# Define server logic required to draw a histogram
server <- function(input, output, session) {
    rv <- reactiveValues(errorMessage="",
                         enrichmentTable=NULL,
                         correlationsTable=NULL,
                         scoresCalculationsTable=NULL,
                         unziped_files=NULL,
                         no_data = TRUE
                         )

    output$no_data <- reactive({
        return(rv$no_data)
    })
    # get files by uploading them
    observeEvent(input$inputUploadedFile, {
        zipfilepath = input$inputUploadedFile[1,"datapath"]
        folderTo = tempdir()
        unzip(zipfilepath, exdir=folderTo)
        rv$unziped_files <- folderTo
    })
    # get files from local path
    observeEvent(input$inputFilePathButton, {
        browser()
        zipfilepath = input$inputFilePath
        if (file.exists(zipfilepath)){
            output$inputFilesPathError <- renderText("File found. Now loading results...")
        }else{
            output$inputFilesPathError <- renderText("File not found")
            return()
        }
        req(zipfilepath)
        folderTo = tempdir()
        unzip(zipfilepath, exdir=folderTo)
        rv$unziped_files <- folderTo
    })

    enrichment_file <- eventReactive(rv$unziped_files,{
        folder = rv$unziped_files
        paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*cell_types_enrichment.txt")[1], sep = "")
    })
    correlations_file <- eventReactive(rv$unziped_files,{
        folder = rv$unziped_files
        paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*correlations.txt")[1], sep = "")
    })
    scores_file <- eventReactive(rv$unziped_files,{
        folder = rv$unziped_files
        paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*score_calculations.txt")[1], sep = "")
    })
    observeEvent(enrichment_file(),{
        req(enrichment_file())
        filepath_enrichments = enrichment_file()
        n = length(filepath_enrichments)
        if(n>0){
            table = read.csv(file = filepath_enrichments, header = TRUE, skip = 33, sep = "\t")
            table = table[table$ews>0,]
            rv$errorMessage <- paste(rv$errorMessage, "\nCell types enrichment uploaded")
            rv$enrichmentTable = table
            output$inputFilesPathError <- NULL
            rv$no_data <- FALSE
        }else{
            rv$errorMessage <- paste(rv$errorMessage, "\nCell types enrichment file not found")
        }
    })
    observeEvent(correlations_file(),{
        req(correlations_file())
        filepath_correlations = correlations_file()
        n = length(filepath_correlations)
        if(n>0){
            table = read.csv(file = filepath_correlations, header = TRUE, sep = "\t")
            rv$errorMessage <- paste(rv$errorMessage, "\nCorrelations uploaded")
            rv$correlationsTable = table
            output$inputFilesPathError <- NULL
        }else{
            rv$errorMessage <- paste(rv$errorMessage, "\nCorrelations file not found")
        }
    })
    observeEvent(scores_file(),{
        req(scores_file())
        filepath_scores = scores_file()
        n = length(filepath_scores)
        if(n>0){
            table = read.csv(file = filepath_scores, header = TRUE, sep = "\t")
            rv$errorMessage <- paste(rv$errorMessage, "\nScore calculations uploaded")
            rv$scoresCalculationsTable = table
        }else{
            rv$errorMessage <- paste(rv$errorMessage, "\nScore calculations file not found")
        }
    })
    output$inputFilesError <- renderText(rv$errorMessage)

    output$enrichmentDataTable <- renderDataTable({
        rv$enrichmentTable
        },
        options = list(pageLength = 20)
    )
    output$correlationsDataTable <- renderDataTable({
        rv$correlationsTable
        },
        options = list(pageLength = 20)
    )
    output$correlationsPlot <- renderPlot(createPlotWithCorrelations(rv$correlationsTable, 0.1))

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
