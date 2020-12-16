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
library(stringi)
library(filesstrings)
# plan(multisession)
plan(multicore)

options(shiny.maxRequestSize = 120*1024^2)

# load("./data/alldata.Rdata")

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
                                             br(),
                                             fluidRow(
                                                 column(width = 12,
                                                        dataTableOutput(outputId = "enrichmentDataTable")
                                                 )
                                             )
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
    source("./server/Table.R", local=TRUE)
    source("./server/Correlations.R", local=TRUE)
    observe({
        query <-parseQueryString(session$clientData$url_search)
        if (!is.null(query$results)) {

            inputFileName <- query$results
            # input file should be in data folder
            zipfilepath = paste('data/', inputFileName, sep = "")

            # check if the file exist
            if (!file.exists(zipfilepath)){
                output$importSideControlUI <- renderUI({
                    tagList(
                        fluidRow(
                            column(width = 12, h4("Opps!"), align='center')
                        )
                    )
                })
                output$importControlUI <- renderUI({
                    tagList(
                        fluidRow(
                            column(width = 12, h4("Sorry, your analysis is not found on the server.")),
                        ),
                        fluidRow(
                            column(width = 12, h5("Please make sure the URL is correct or contact your administrator.")),
                        )
                    )
                })
                return()
            }

            # side panel
            output$importSideControlUI <- renderUI({
                tagList(
                    fluidRow(
                        column(width = 12, h4("Analysis from:"), align='center')
                    ),
                    fluidRow(
                        column(width = 12, h6(tools::file_path_sans_ext(basename(inputFileName))), align='center')
                    )
                )
            })
            url <- paste(session$clientData$url_protocol, "//", session$clientData$url_hostname, ":", session$clientData$url_port, session$clientData$url_pathname, sep = "")
            output$importControlUI <- renderUI({
                tagList(
                    p("Your dataset is already imported in the results viewer."),
                    p("Explore the other tabs to see your data."),
                    p("Also, you can download your results here:"),
                     downloadButton(outputId = 'downloadData', label = "Download Zip"),
                    p(),p("You can then go back to this URL, upload that file, and import it to see the results."),
                    fluidRow(
                        column(width = 12, a(url, href = url), align = 'left')
                    )
                )
            })

            # create download data button
            output$downloadData <- downloadHandler(
                filename = inputFileName,
                content = function(file){
                    file.copy(zipfilepath, file)
                }
            )

            # unzip if not already unziped
            folderTo <- paste(dirname(zipfilepath), "/", tools::file_path_sans_ext(basename(zipfilepath)), sep = "")
            if (!file.exists(folderTo)){
                withProgress({
                    setProgress(message = "Unzipping results...", value = 0)
                    unzip(zipfilepath, exdir = folderTo)
                    setProgress(message = "Results unzipped", value = 1)
                    rv$unziped_files <- folderTo
                },
                detail = "This just will take a few seconds"
                )
            }else{
                rv$unziped_files <- folderTo
            }



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
                         scoresCalculationsTable=NULL,
                         unziped_files=NULL
    )
    unziped_files <- reactiveVal()
    output$data_loaded <- reactive({FALSE})
    outputOptions(output, "data_loaded", suspendWhenHidden = FALSE)

    # get files by uploading them and unzip them
    observeEvent(input$inputUploadedFile, {
        file <- input$inputUploadedFile
        zipfilepath = file$datapath
        # copy file to data
        newZipFilepath <- paste("data/", file$name, sep = "")
        file.move(files = c(zipfilepath) , destinations = "data/", overwrite = TRUE)
        file.rename(from = paste("data/", basename(zipfilepath), sep = ""), to = newZipFilepath)
        folderTo <- paste("data/", tools::file_path_sans_ext(basename(file$name)), sep = "")
        withProgress({
            setProgress(message = "Unzipping results...", value = 0)
            unzip(newZipFilepath, exdir = folderTo)
            setProgress(message = "Results unzipped", value = 1)
            unziped_files(folderTo)
        },
        detail = "This just will take a few seconds"
        )
    })



    # select the scores file
    scores_file <- eventReactive(unziped_files(),{
        browser()
        folder = unziped_files()
        paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*score_calculations.txt")[1], sep = "")
    })
    # read the file
    # scores_table <- eventReactive(scores_file(),{
    #     withProgress({
    #          table = read.csv(file = scores_file(), header = TRUE, sep = "\t")
    #     })
    # })
    output$inputDataError <- renderText(rv$errorMessage)





    output$cellTypeCorrelationsPlot <- renderPlot(createPlotWithCorrelations(isolate({rv$correlationsTable}), 0.1, input$selectCellType))

    output$cellTypeScoreCalculationPlot <- renderPlot(createPlotWithScoreCalculation(isolate({rv$scoresCalculationsTable}), input$selectCellType))


}



# Run the application
shinyApp(ui = ui, server = server, options = ( launch.browser = TRUE))



