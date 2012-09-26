function valid_form() {
    return $("#user").val() &&
           $("#repo").val();
}

function decorate_repo_links() {
    $(".proj").click(function() {
        var user_repo = $(this).attr("id").split("/");
        ajax_update_page(user_repo[0], user_repo[1]);
        return false;
    });

}

function ajax_update_page(user, repo) {
    $.ajax({url:"/update/",
        data: {user: user, repo: repo},
        success: function(data) {
            $("#rawdata").html("<pre>" + JSON.stringify(data.author_data, undefined, 2) + "</pre>");
            $("#repolist").html("");
            for(var el in data.current_repos) {
                var cur = data.current_repos[el];
                var user_repo = cur[0] + "/" + cur[1];
                $("#repolist").append(
                        "<p>" +
                        "<a class='proj' " +
                        "id='" + user_repo + "' " +
                        "href='" + user_repo +"'>" +
                        user_repo +
                        "</a>" +
                        "</p>");
            }
            decorate_repo_links();
        }
    });
}

$(function() {
    $("#submit").click(function() {
        if(!valid_form()) {
            return false;
        }
        ajax_update_page($("#user").val(), $("#repo").val());
        return false;
    });
    decorate_repo_links();
});
