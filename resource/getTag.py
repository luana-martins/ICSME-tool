import github3
import pandas as pd

csv_projects = pd.read_csv("C:/Users/luana/Downloads/repo.csv")
gh = github3.login(token='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')

for index, row in csv_projects.iterrows():
  repository = gh.repository(owner=row['OWNER'],
                            repository=row['REPO'])
  for tag in repository.tags():
      assert isinstance(tag, github3.repos.tag.RepoTag)
      print('{} @ {} @ {} @ {} @ {}'.format(row['ID'], row['OWNER'], row['REPO'], tag.name, tag.commit.sha))
